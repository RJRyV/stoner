package stoner.sgfParser

import stoner.board.{Side, StateTransition, WHITE, BLACK, EMPTY, PosFlip,Move,Game,Board}

import scala.collection.mutable.ArrayBuffer

object SGFStringParser {
  
  val sideMap = Map('B' -> BLACK, 'W' -> WHITE)
  
  def parseLines(lines : List[String]) : Option[Game] = {
    if (!lines.head.startsWith("(;SZ[19]")) None
    else {
      val st_o = new ArrayBuffer[StateTransition]
      var winner_o = EMPTY
      
      for(line <- lines.tail) {
        if(line.startsWith("RE")) {
          if(line(3) == 'B') winner_o = BLACK
          else winner_o = WHITE
        }
        else if(line.startsWith("AB")) {
          for(s <- line.drop(1).sliding(2, 4)) {
            st_o.append(PosFlip(PositionTranslator.strRepToPos(s), BLACK))
          }
        }
        else if(line.startsWith(";")) {
          val moves = line.split(';').filter(_.size > 0)
          st_o ++ moves.map((m : String) => Move(PositionTranslator.strRepToPos(m.substring(2,4)),
                                                 sideMap(m(0))))
        }
      }
      
      Some(Game(new Board(st_o), winner_o))
    }
  }//end def parseLines(lines : List[String])

}//end object SGFStringParser

//31337