/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.river;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import net.dries007.tfc.world.noise.NoiseUtil;


public abstract class AbstractRiverGenerator
{
    public static final int RIVER_CHUNK_RADIUS = 16; // The maximum bounding box of a river generation
    public static final float RIVER_START_WEIGHT = 1f;
    public static final float BRANCH_CHANCE = 0.7f;
    public static final int DRAIN_RIVER_WIDTH = 20; // The width of each river at the drain element (plus or minus a range)
    public static final int SOURCE_CUTOFF_WIDTH = 8; // The width at which a branch must end with a source element
    public static final int SOURCE_MAX_WIDTH = 12; // The width at which a branch *may* end with a source element
    public static final int BRANCH_DECREASE_WIDTH = 2; // How much branch with decreases relative to the main branch

    private static final Logger LOGGER = LogManager.getLogger();

    protected final Map<ChunkPos, RiverStructure> generatedRiverStructures; // Positions and rivers that have been generated, cached here for efficiency
    protected final Random random;
    protected final long seed;

    public AbstractRiverGenerator(long seed)
    {
        this.seed = seed;
        this.generatedRiverStructures = new HashMap<>();
        this.random = new Random();
    }

    /**
     * Generates rivers starting at a given chunk
     * This method is not completely deterministic - it can be influenced by the order chunks are explored, since
     * it takes into account existing generated rivers and avoids intersections
     *
     * @param posAt The position of the origin of the river.
     * @return the river that was generated, or null if not
     */
    @Nullable
    public RiverStructure generateRiverAtChunk(ChunkPos posAt)
    {
        random.setSeed(NoiseUtil.hash(seed, posAt.x, posAt.z));
        if (!(random.nextFloat() > RIVER_START_WEIGHT))
        {
            // River structure
            RiverStructure river = new RiverStructure(posAt.getMinBlockX(), posAt.getMinBlockZ());
            List<RiverBranch> branches = new ArrayList<>(); // track possible branches
            List<RiverBranch> potentialBranches = new ArrayList<>(); // potential branches off an uncommitted river branch

            // Drain piece
            RiverPiece drainPiece = generateDrainPiece(posAt);
            if (drainPiece != null)
            {
                // Start with the main branch, iterate through adding all branches
                branches.add(new RiverBranch(drainPiece));
                boolean validBranchesAdded = false;
                while (!branches.isEmpty())
                {
                    // Generate a branch to completion, or to end
                    RiverBranch branch = branches.remove(0);
                    potentialBranches.clear();
                    if (generateBranch(river, branch, potentialBranches))
                    {
                        river.addBranch(branch);
                        branches.addAll(potentialBranches);
                        validBranchesAdded = true;
                    }
                }

                if (validBranchesAdded)
                {
                    LOGGER.info("Generated river at {}", posAt);
                    river.markGenerated();
                    generatedRiverStructures.put(posAt, river);
                    return river;
                }
            }
        }
        return null;
    }

    /**
     * Tries to generate a valid river starting point
     * @param posAt The chunk pos of the river start
     * @return A template for the river start if valid, null if not
     */
    @Nullable
    protected RiverPiece generateDrainPiece(ChunkPos posAt)
    {
        BlockPos pos = findValidDrainPos(posAt);
        if (pos != null)
        {
            List<RiverTemplate> drainTemplates = RiverTemplate.DRAIN_TEMPLATES.get(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            RiverTemplate drainTemplate = drainTemplates.get(random.nextInt(drainTemplates.size()));
            RiverPiece drainPiece = new RiverPiece(drainTemplate, pos.getX() - drainTemplate.getDownstreamX(), pos.getZ() - drainTemplate.getDownstreamZ(), DRAIN_RIVER_WIDTH, getSeaLevel());

            // Check that drain does not intersect other rivers
            for (RiverStructure otherRiver : generatedRiverStructures.values())
            {
                if (otherRiver.intersectsBox(drainPiece.getBox()))
                {
                    return null;
                }
            }
            if (isValidPiece(drainPiece))
            {
                return drainPiece;
            }
        }
        return null;
    }

    @Nullable
    protected abstract BlockPos findValidDrainPos(ChunkPos pos);

    /**
     * Generates a single river branch
     * Returns if the branch is valid once generated
     */
    protected boolean generateBranch(RiverStructure river, RiverBranch branch, List<RiverBranch> otherBranches)
    {
        RiverPiece downstreamPiece = branch.getJoinPiece();
        while (downstreamPiece.getWidth() > SOURCE_CUTOFF_WIDTH)
        {
            int nextWidth = downstreamPiece.getWidth() - 1;
            if (nextWidth == SOURCE_CUTOFF_WIDTH)
            {
                // Must generate a source piece
                RiverPiece sourcePiece = generateSourcePiece(river, branch, downstreamPiece, SOURCE_CUTOFF_WIDTH);
                if (sourcePiece != null && !branch.intersectsBoxIgnoringPiece(sourcePiece.getBox(), downstreamPiece))
                {
                    // Valid source piece
                    branch.push(sourcePiece);
                    return true;
                }
                // Can't generate a source piece, so instead try and replace the previous piece with a source instead
                return replaceLastPieceWithSource(river, branch, downstreamPiece);
            }
            else
            {
                // Generate possible straight and branch pieces
                RiverPiece straightPiece = generateStraightPiece(river, branch, downstreamPiece, nextWidth);
                if (straightPiece != null)
                {

                    // Optionally, generate a tributary
                    if (random.nextFloat() < BRANCH_CHANCE)
                    {
                        RiverPiece branchPiece = generateBranchPiece(river, branch, downstreamPiece, nextWidth - BRANCH_DECREASE_WIDTH, straightPiece.getUpstreamDirection());
                        if (branchPiece != null && branchPiece.getTemplate() != straightPiece.getTemplate() && branchPiece.getUpstreamDirection() != straightPiece.getUpstreamDirection())
                        {
                            // Branch is valid: can't be the same piece as the normal, or have the same direction upstream
                            RiverBranch newBranch = new RiverBranch(branchPiece);
                            otherBranches.add(newBranch);
                        }
                    }

                    branch.push(straightPiece);
                    downstreamPiece = straightPiece;
                }
                else
                {
                    // this piece doesn't fit for some reason - try and turn this branch into a source by replacing the last piece (which was valid)
                    return replaceLastPieceWithSource(river, branch, downstreamPiece);
                }
            }
        }
        return false;
    }

    protected abstract int getSeaLevel();

    /**
     * When a river branch fails to generate next pieces, it tries to replace the last added piece with a source piece
     * Returns true if it succeeds and the river has a valid source piece
     */
    protected boolean replaceLastPieceWithSource(RiverStructure river, RiverBranch branch, RiverPiece downstreamPiece)
    {
        // piece to be replaced must be at most the max width for a source piece
        if (downstreamPiece.getWidth() <= SOURCE_MAX_WIDTH && downstreamPiece.getDownstream() != null && branch.getPieces().size() > 1)
        {
            RiverPiece sourcePiece = generateSourcePiece(river, branch, downstreamPiece.getDownstream(), downstreamPiece.getWidth());
            if (sourcePiece != null)
            {
                // Source is valid, so replace the previous piece
                branch.pop();
                branch.push(sourcePiece);
                return true;
            }
        }
        return false;
    }

    @Nullable
    protected RiverPiece generateBranchPiece(RiverStructure river, RiverBranch branch, RiverPiece previousPiece, int riverWidth, Direction excludedDirection)
    {
        return generatePiece(river, branch, previousPiece, riverWidth, RiverTemplate.CONNECTOR_TEMPLATES.get(previousPiece.getUpstreamDirection()).stream().filter(template -> template.getUpstreamDirection() != excludedDirection).collect(Collectors.toList()));
    }

    @Nullable
    protected RiverPiece generateStraightPiece(RiverStructure river, RiverBranch branch, RiverPiece previousPiece, int riverWidth)
    {
        return generatePiece(river, branch, previousPiece, riverWidth, RiverTemplate.CONNECTOR_TEMPLATES.get(previousPiece.getUpstreamDirection()));
    }

    @Nullable
    protected RiverPiece generateSourcePiece(RiverStructure river, RiverBranch branch, RiverPiece previousPiece, int riverWidth)
    {
        return generatePiece(river, branch, previousPiece, riverWidth, RiverTemplate.SOURCE_TEMPLATES.get(previousPiece.getUpstreamDirection()));
    }

    @Nullable
    protected RiverPiece generatePiece(RiverStructure river, RiverBranch branch, RiverPiece previousPiece, int riverWidth, List<RiverTemplate> templates)
    {
        RiverTemplate template = templates.get(random.nextInt(templates.size()));
        RiverPiece piece = new RiverPiece(template, previousPiece, riverWidth, previousPiece.getHeight() + 1);

        // check bounding box doesn't intersect this river, and the branch doesn't intersect itself, and that the piece is withing the generation range for the river
        if (!river.intersectsBoxIgnoringPiece(piece.getBox(), previousPiece) && !branch.intersectsBoxIgnoringPiece(piece.getBox(), previousPiece) && piece.getBox().containedIn(river.getBoundingBox()))
        {
            // Check all other possible river intersections that have been generated
            for (RiverStructure otherRiver : generatedRiverStructures.values())
            {
                if (otherRiver.intersectsBox(piece.getBox()))
                {
                    return null;
                }
            }

            if (isValidPiece(piece))
            {
                return piece;
            }
        }
        // Unable to find a matching piece
        return null;
    }

    protected abstract boolean isValidPiece(RiverPiece piece);
}
