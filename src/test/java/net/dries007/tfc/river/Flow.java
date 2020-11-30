package net.dries007.tfc.river;

public enum Flow
{
    // The order of these is important, as we do ordinal based rotations and averages
    NNN(0, -2), // 0
    NNE(1, -2),
    N_E(1, -1),
    NEE(2, -1),
    EEE(2, 0), // 4
    SEE(2, 1),
    S_E(1, 1),
    SSE(1, 2),
    SSS(0, 2), // 8
    SSW(-1, 2),
    S_W(-1, 1),
    SWW(-2, 1),
    WWW(-2, 0), // 12
    NWW(-2, -1),
    N_W(-1, -1),
    NNW(-1, -2), // 15
    ___(0, 0);

    public static final Flow NONE = ___; // This is easier to read than ___ in some variable names, but we want to keep ___ for clarity in template declarations

    private static final Flow[] VALUES = values();
    private static final int MODULUS = VALUES.length - 1; // Since when taking modulo, we want to skip NONE

    public static Flow rotateCW(Flow original)
    {
        // Rotates by ordinal
        if (original == NONE)
        {
            return original;
        }
        else
        {
            int newOrdinal = (original.ordinal() + 4) % MODULUS;
            return VALUES[newOrdinal];
        }
    }

    public static Flow mirrorX(Flow original)
    {
        if (original == NONE || original == NNN || original == SSS)
        {
            return original;
        }
        else
        {
            int newOrdinal = -original.ordinal() + MODULUS;
            return VALUES[newOrdinal];
        }
    }

    /**
     * Averages four flows from the corners of a square, using two weights to describe the location within the square.
     */
    public static Flow combine(Flow flowNE, Flow flowSE, Flow flowNW, Flow flowSW, float weightE, float weightN)
    {
        Flow flowN = combine(flowNE, flowNW, weightE, flowSE != NONE && flowSW != NONE);
        Flow flowS = combine(flowSE, flowSW, weightE, flowNE != NONE && flowNW != NONE);
        return combine(flowN, flowS, weightN, false);
    }

    /**
     * Averages two flows with a weighted value.
     *
     * @param preventNone if true, this will default to not return none, unless both left and right are none
     */
    public static Flow combine(Flow left, Flow right, float weightLeft, boolean preventNone)
    {
        if (left == NONE)
        {
            return preventNone || weightLeft < 0.5 ? right : NONE;
        }
        else if (right == NONE)
        {
            return preventNone || weightLeft > 0.5 ? left : NONE;
        }
        else
        {
            int ordinalDistance = Math.abs(left.ordinal() - right.ordinal());
            if (ordinalDistance == 8)
            {
                // exact opposites
                return weightLeft > 0.5 ? left : right;
            }
            else if (ordinalDistance < 8)
            {
                // The center is the correct average
                int newOrdinal = (int) (left.ordinal() * weightLeft + right.ordinal() * (1 - weightLeft));
                return VALUES[newOrdinal];
            }
            else
            {
                // We need to average outside the center, by shifting the smaller one, averaging, and then taking a modulo
                int leftValue = left.ordinal(), rightValue = right.ordinal();
                if (leftValue < rightValue)
                {
                    leftValue += 16;
                }
                else
                {
                    rightValue += 16;
                }
                int newOrdinal = (int) (leftValue * weightLeft + rightValue * (1 - weightLeft));
                return VALUES[newOrdinal % MODULUS];
            }
        }
    }

    final int x, z;

    Flow(int x, int z)
    {
        this.x = x;
        this.z = z;
    }
}
