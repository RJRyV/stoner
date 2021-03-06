package stoner.board

import scala.reflect.ClassTag

import scala.annotation.tailrec

import scala.collection.immutable.{HashSet,Set}
import scala.collection.LinearSeq
import java.util.Arrays

import org.apache.spark.mllib.linalg.{DenseVector, SparseVector}

/** A representation of stones on a Go board and the count of each players'
 *  captured stones.
 * 
 */
trait Grid {
  
  val boardDimension : BoardDimension
  
  /**The number of black stones that have been captured.*/
  val capturedBlack : Int
  /**The number of white stones that have been captured.*/
  val capturedWhite : Int
  
  /**
   * Gets the Side stored in the grid for the given Position.
   * 
   * @param pos The position to get the side for
   * @return The side for the given position
   */
  def get(pos: Position) : Side
  
  /**
   * Sets the side at the given position and returns a new CompactGrid 
   * representing the updated state.
   * 
   * @param pos The position to set the Side for
   * @param side The Side value to set at the given position
   * 
   * @return A new CompactGrid with the state updated at the specified side and
   *  pos
   */
  def set(pos : Position, side : Side) : Grid

  def setCapturedBlack(captured: Int) : Grid
  def setCapturedWhite(captured: Int) : Grid

  
  /**
   * Gets the Side stored in the grid for the given column and row.
   * 
   * @param column The column to get the side for
   * @param row The row to get the side for
   * @return The side for the given column and row combination.
   */
  def get(column : Dimension, row : Dimension) : Side = get(Position(column, row))
  
  def apply(column : Dimension, row : Dimension) : Side = get(column,row)
  
  def set(pf: PosFlip) : Grid = pf match {case PosFlip(p,s) => set(p,s)}
  
  def set(pfs : LinearSeq[PosFlip]) : Grid = (this /: pfs)(_ set _)
  
  def isEmpty(pos : Position) : Boolean = get(pos) == EMPTY 
  
  /**
   * Determines whether or not the given Position is legally within the 
   * dimensions of the grid.
   * @param pos The Position to evaluate for legality
   * @return True if the given position is within the confines of boardDimension,
   *  false otherwise.
   */
  def isLegalPosition(pos: Position) : Boolean = pos match {
    case Position(col,row) => 
      col >=0 && col < boardDimension.column &&
      row >=0 && row < boardDimension.row
  }//end def isLegalPosition(pos: Position) : Boolean = pos match
  
  protected def iterateAllPossibleNeighbors(pos: Position) : Set[Position] = 
    pos match { case Position(col,row) => Set[Position](Position(col-1, row),
	                                                      Position(col+1, row),
		     		                                            Position(col, row-1),
				                                                Position(col, row+1))
	}
  
  /**
   * Gets the legal neighboring Positions of the given Position
   * @param The Position to get the legal neihbors for
   * @return All of the neighbors of the given Position that are on the grid. 
   */
  def getNeighbors(pos : Position) : Set[Position] = 
    (iterateAllPossibleNeighbors(pos)).filter(isLegalPosition)

  /**
   * Returns a Set of Positions that represent the liberties of the stone at 
   * the given position.  
   * 
   * @param pos The Position of one stone in a group.
   * 
   * @return A Set of Positions representing the liberties of a stone at 
   * Position pos, an empty Set if the group has no liberties.
   *   
   */
  def liberties(pos : Position) : Set[Position] = 
    getNeighbors(pos).filter((p) => get(p) == EMPTY)
    
  /**
   * Identifies the Position of all stones that are part of the same group as
   * the stone at the given position.
   * @param pos The Position holding the stone
   * @return The Positions (including the pos parameter) of all stones that are
   * the same side as the stone at pos if pos is occupied, empty Set otherwise.
   */
  def identifyGroup(pos: Position) : Set[Position] = {
    
    val side = get(pos)
    
    @tailrec
    def idGroupRec(posToSearch: Set[Position],
                   acc: Set[Position]) : Set[Position]= {
      if(posToSearch.isEmpty) acc
      else {
    	  val h = posToSearch.head
    		val t = posToSearch.tail

    		//state farm
    		def goodNeighbor(p: Position) = get(p) == side && !acc.contains(p) 

    		idGroupRec(getNeighbors(h).filter(goodNeighbor) ++ t,acc + h)
      }//end else to if(posToSearch.isEmpty)
    }//end def idGroupRec(pos: Position, side: Side, acc: Set[Position])

    idGroupRec(HashSet[Position](pos), new HashSet[Position]())
      
  }//end def findGroup(pos: Position, side: Side)
  
  /**
   * Counts the number of liberties of the group associated with the stone 
   * at the given Position.
   * 
   * @param pos The Position of one stone in a group
   * 
   * @return The Set of Positions representing the liberties of the group 
   * associated with the stone at pos.  An empty Set if the group has no
   * liberties.
   * 
   */
  def groupLiberties(pos: Position) : Set[Position] = {
    identifyGroup(pos).flatMap(liberties)
  }//end def countLiberties(pos: Position) : Set[Position]
  
  /**Determines whether or not the group associated with the stone at Position
   * pos is alive, e.g. has at least one liberty.
   * 
   *  ("While I thought that I was learning how to live, I have been learning
   *  how to die" - Benjamin Franklin).
   *  
   *  @param pos The Position of one stone in a group
   *  
   *  @return True if the group associated with the stone at pos is alive, 
   *   false otherwise.
   *   
   */
  def isAlive(pos: Position) : Boolean = !groupLiberties(pos).isEmpty

  /**
   * Iterates over all possible positions in the Grid.
   */
  def iteratePositions = {
    (for(c <- Range(0,boardDimension.column);
         r <- Range(0,boardDimension.row))
      yield Position(c,r))
  }
  
  def allPositions : Set[Position] = HashSet() ++ iteratePositions
  
  /**
   * Flattens the internal representation of the grid into a 1-D array of Sides.
   * The flattening happens in column major order, e.g. the first column is 
   * prepended to the second which is prepended to the third ...
   */
  def flatten : IndexedSeq[Side] = iteratePositions map get
      
  private sealed trait FromSide[A] {
    def apply(side: Side): A
  }
  
  private object FromSide {
    implicit object intFromSide extends FromSide[Int] {
      def apply(side : Side) = side.toInt
    }
    
    implicit object doubleFromSide extends FromSide[Double] {
      def apply(side : Side) = side.toDouble
    }
    
    implicit object charFromSide extends FromSide[Char] {
      def apply(side : Side) = side.toChar
    }
  }
  
  /**
   * Flattens the internal represenation of the grid into a 1-D array of numeric
   * values. The flattening happens in column major order, e.g. the first column 
   * is prepended to the second which is prepended to the third ...
   */
  def flattenNumeric[T : FromSide : Manifest] : IndexedSeq[T] =
    flatten.map(implicitly[FromSide[T]].apply)
      
  /**
   * Flattens the grid into a 1D spark Vector (a vector of features).
   * 
   * @return A 1D org.apache.spark Vector representation of the Grid.
   */
  def flattenSparkVector : org.apache.spark.mllib.linalg.Vector = {
    
    val a  = flattenNumeric[Double]
    
    val L = a.length
    val I = 32 // bits in an int
    val D = 64 // bits in a double
    val N = a.count(_ != 0.0) // number of items
    
    //For a description of the below equation please see
    //https://github.com/RJRyV/stoner/wiki/Compact-Memory-Storage-of-Go-Positions-In-Scala-2:-Vector-Day
    if (N < (L*D - I) / (I+D)) {
      val arrWithInd = a.zipWithIndex.filter(_._1 != 0.0)
      new SparseVector(a.length, arrWithInd.map(_._2).toArray, arrWithInd.map(_._1).toArray)
    }
    else {
      new DenseVector(flattenNumeric[Double].toArray)
    }
      
    
  }//end def flattenSparkVector : Vector
    
  /**
   * Provides a "deep" hashCode of the grid, i.e. based on contents.
   * 
   * @return A hash value that is based on the contents of the grid.  The 
   * collision probability of two disimilar grids is 1/2^32.
   */
  @Override
  override def hashCode : Int = Arrays.hashCode(flattenNumeric[Int].toArray)
  
  /**
   * Provides a "deep" equality check based on the grid.
   * 
   * @return True if the contents of the other grid are equal to the contents
   * of this Grid, false otherwise (more than likely).
   */
  @Override
  override def equals(o: Any) = o match {
    case that: Grid => that.hashCode == hashCode
    case _ => false
  }
  
  /**
   * Prints a pretty representation of the Board.
   */
  override def toString  = {
    val lines = 
      for {
        r <- Range(0, boardDimension.row)
      } yield Range(0,boardDimension.column).map(get(_,r).toChar).mkString(" ")
      
    lines.mkString("\n")
  }
  
}//end trait Grid

//31337