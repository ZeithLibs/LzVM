package dev.zeith.lzvm.program;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ArgType
{
	DOUBLE("d"),
	STRING("s"),
	;
	
	public final String desc;
}
