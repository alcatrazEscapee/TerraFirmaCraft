/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.river;

import java.util.List;
import java.util.Stack;

/**
 * This is a temporary holding object for uncommitted sections of river structures
 * In order to be committed, a branch must have a valid source piece at it's end
 * If such a piece is unable to be found, the branch is rejected, discarding all potential pieces it has generated
 */
public class RiverBranch implements IRiverStructure
{
    private final RiverPiece joinPiece;
    private final Stack<RiverPiece> pieces;

    public RiverBranch(RiverPiece joinPiece)
    {
        this.joinPiece = joinPiece;
        this.pieces = new Stack<>();
        this.pieces.push(joinPiece);
    }

    public void push(RiverPiece piece)
    {
        this.pieces.push(piece);
    }

    public void pop()
    {
        this.pieces.pop();
    }

    public RiverPiece getJoinPiece()
    {
        return joinPiece;
    }

    @Override
    public List<RiverPiece> getPieces()
    {
        return pieces;
    }
}
