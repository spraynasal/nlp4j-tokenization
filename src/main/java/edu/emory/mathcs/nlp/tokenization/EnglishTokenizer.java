/**
 * Copyright 2014, Emory University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.emory.mathcs.nlp.tokenization;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.nlp.common.constant.CharConst;
import edu.emory.mathcs.nlp.common.util.Language;
import edu.emory.mathcs.nlp.common.util.StringUtils;
import edu.emory.mathcs.nlp.component.template.node.NLPNode;
import edu.emory.mathcs.nlp.component.template.util.NLPUtils;
import edu.emory.mathcs.nlp.tokenization.dictionary.Abbreviation;
import edu.emory.mathcs.nlp.tokenization.dictionary.Compound;
import edu.emory.mathcs.nlp.tokenization.dictionary.EnglishApostrophe;
import edu.emory.mathcs.nlp.tokenization.dictionary.EnglishHyphen;
import edu.emory.mathcs.nlp.tokenization.util.TokenIndex;

/**
 * @since 3.0.0
 * @author Jinho D. Choi ({@code jinho.choi@emory.edu})
 */
public class EnglishTokenizer extends Tokenizer
{
	private final String[] L_BRACKETS = {"\"","(","{","["};
	private final String[] R_BRACKETS = {"\"",")","}","]"};
	
	private EnglishApostrophe d_apostrophe;
	private Abbreviation      d_abbreviation;
	private Compound          d_compound;
	private EnglishHyphen     d_hyphen;
	
	public EnglishTokenizer()
	{
		d_apostrophe   = new EnglishApostrophe();
		d_abbreviation = new Abbreviation();
		d_compound     = new Compound(Language.ENGLISH);
		d_hyphen       = new EnglishHyphen();
	}
	
//	----------------------------------- Tokenize -----------------------------------
	
	@Override
	protected int adjustFirstNonSymbolGap(char[] cs, int beginIndex, String t)
	{
		return 0;
	}
	
	@Override
	protected int adjustLastSymbolSequenceGap(char[] cs, int endIndex, String t)
	{
		char sym = cs[endIndex];
		
		if (sym == CharConst.PERIOD)
		{
			if (d_abbreviation.isAbbreviationEndingWithPeriod(StringUtils.toLowerCase(t)))
				return 1;
		}
		
		return 0;
	}

	@Override
	protected boolean preserveSymbolInBetween(char[] cs, int index)
	{
		return d_hyphen.preserveHyphen(cs, index);
	}
	
	@Override
	protected boolean tokenizeWordsMore(List<NLPNode> tokens, String original, String lower, char[] lcs, TokenIndex bIndex2)
	{
		return tokenize(tokens, original, lower, lcs, d_apostrophe, bIndex2) || tokenize(tokens, original, lower, lcs, d_compound, bIndex2); 
	}
	
//	----------------------------------- Segmentize -----------------------------------
	
	@Override
	public List<NLPNode[]> segmentize(List<NLPNode> tokens)
	{
		List<NLPNode[]> sentences = new ArrayList<>();
		int[] brackets = new int[R_BRACKETS.length];
		int bIndex, i, size = tokens.size();
		boolean isTerminal = false;
		String token;
		
		for (i=0, bIndex=0; i<size; i++)
		{
		    token = tokens.get(i).getWordForm();
			countBrackets(token, brackets);
			
			if (isTerminal || isFinalMarksOnly(token))
			{
				if (i+1 < size && isFollowedByBracket(tokens.get(i + 1).getWordForm(), brackets))
				{
					isTerminal = true;
					continue;
				}
				
				sentences.add(NLPUtils.toNodeArray(tokens, bIndex, bIndex = i+1));
				isTerminal = false;
			}
		}
		
		if (bIndex < size)
			sentences.add(NLPUtils.toNodeArray(tokens, bIndex, size));

		return sentences;
	}
		
	/** Called by {@link EnglishSegmenter#getSentencesRaw(BufferedReader)}. */
	private void countBrackets(String str, int[] brackets)
	{
		if (str.equals("\""))
			brackets[0] += (brackets[0] == 0) ? 1 : -1;
		else
		{
			int i, size = brackets.length;
			
			for (i=1; i<size; i++)
			{
				if      (str.equals(L_BRACKETS[i]))
					brackets[i]++;
				else if (str.equals(R_BRACKETS[i]))
					brackets[i]--; 
			}
		}
	}
	
	/** Called by {@link EnglishSegmenter#getSentencesRaw(BufferedReader)}. */
	private boolean isFollowedByBracket(String str, int[] brackets)
	{
		int i, size = R_BRACKETS.length;
		
		for (i=0; i<size; i++)
		{
			if (brackets[i] > 0 && str.equals(R_BRACKETS[i]))
				return true;
		}
		
		return false;
	}
}
