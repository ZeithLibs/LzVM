package dev.zeith.lzvm.molang.parcelets.prefix;

import dev.zeith.lzvm.exception.LzVMException;
import dev.zeith.lzvm.molang.expression.*;
import dev.zeith.lzvm.molang.parcelets.IPrefixParselet;
import dev.zeith.lzvm.molang.parser.MoParser;
import dev.zeith.lzvm.molang.tokenizer.Token;

import java.util.ArrayList;

public class LoopParselet
		implements IPrefixParselet
{
	@Override
	public MLExpression parse(MoParser parser, Token token)
	{
		ArrayList<MLExpression> args = parser.parseArgs();
		if(args.size() != 2) throw new LzVMException("loop() expects 2 arguments (loop_count, {code}), " + args.size() + " aguments provided.");
		return new LoopExpression(args.get(0), args.get(1));
	}
}