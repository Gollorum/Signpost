package gollorum.signpost.security;

import gollorum.signpost.BlockRestrictions;

public interface WithCountRestriction {

	BlockRestrictions.Type getBlockRestrictionType();

}
