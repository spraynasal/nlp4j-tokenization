/**
 * Copyright 2015, Emory University
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
package edu.emory.mathcs.nlp.bin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kohsuke.args4j.Option;

import edu.emory.mathcs.nlp.common.constant.StringConst;
import edu.emory.mathcs.nlp.common.util.BinUtils;
import edu.emory.mathcs.nlp.common.util.FileUtils;
import edu.emory.mathcs.nlp.common.util.IOUtils;
import edu.emory.mathcs.nlp.common.util.Joiner;
import edu.emory.mathcs.nlp.common.util.Language;
import edu.emory.mathcs.nlp.tokenizer.Tokenizer;

/**
 * @author Jinho D. Choi ({@code jinho.choi@emory.edu})
 */
public class Tokenize
{
	@Option(name="-l", usage="language (default: english)", required=false, metaVar="<language>")
	private String language = Language.ENGLISH.toString();
	@Option(name="-i", usage="input path (required)", required=true, metaVar="<filepath>")
	private String input_path;
	@Option(name="-ie", usage="input file extension (default: *)", required=false, metaVar="<regex>")
	private String input_ext = "*";
	@Option(name="-oe", usage="output file extension (default: tok)", required=false, metaVar="<string>")
	private String output_ext = "tok";
	@Option(name="-line", usage="if set, treat each line as one sentence", required=false, metaVar="<boolean>")
	private boolean line_base = false;
	@Option(name="-threads", usage="number of threads (default: 2)", required=false, metaVar="<integer>")
	protected int thread_size = 2;
	
	public Tokenize() {}
	
	public Tokenize(String[] args)
	{
		BinUtils.initArgs(args, this);
		
		Tokenizer tokenizer = Tokenizer.create(Language.getType(language));
		ExecutorService executor = Executors.newFixedThreadPool(thread_size);
		String outputFile;
		
		for (String inputFile : FileUtils.getFileList(input_path, input_ext, false))
		{
			outputFile = inputFile + StringConst.PERIOD + output_ext;
			executor.submit(new NLPTask(tokenizer, inputFile, outputFile));
		}
		
		executor.shutdown();
	}
	
	public void tokenizeRaw(Tokenizer tokenizer, String inputFile, String outputFile) throws IOException
	{
		InputStream in  = IOUtils.createFileInputStream(inputFile);
		PrintStream out = IOUtils.createBufferedPrintStream(outputFile);
		
		for (List<String> tokens : tokenizer.segmentize(in))
			out.println(Joiner.join(tokens, StringConst.SPACE));
		
		in.close();
		out.close();
	}
	
	public void tokenizeLines(Tokenizer tokenizer, String inputFile, String outputFile) throws IOException
	{
		BufferedReader reader = IOUtils.createBufferedReader(inputFile);
		PrintStream out = IOUtils.createBufferedPrintStream(outputFile);
		String line;
		
		while ((line = reader.readLine()) != null)
			out.println(Joiner.join(tokenizer.tokenize(line), StringConst.SPACE));
		
		reader.close();
		out.close();
	}
	
	class NLPTask implements Runnable
	{
		private Tokenizer tokenizer;
		private String    input_file;
		private String    output_file;
		
		public NLPTask(Tokenizer tokenizer, String inputFile, String outputFile)
		{
			this.tokenizer   = tokenizer;
			this.input_file  = inputFile;
			this.output_file = outputFile;
		}
		
		@Override
		public void run()
		{
			try
			{
				BinUtils.LOG.info(FileUtils.getBaseName(input_file)+"\n");
				if (line_base) tokenizeLines(tokenizer, input_file, output_file);
				else		   tokenizeRaw  (tokenizer, input_file, output_file);
			}
			catch (Exception e) {e.printStackTrace();}
		}
	}
	
	static public void main(String[] args)
	{
		new Tokenize(args);
	}
}