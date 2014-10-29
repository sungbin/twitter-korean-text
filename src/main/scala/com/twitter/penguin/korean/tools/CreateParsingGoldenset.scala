package com.twitter.penguin.korean.tools

import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

import com.twitter.penguin.korean.TwitterKoreanProcessor._
import com.twitter.penguin.korean.thriftscala._
import com.twitter.penguin.korean.util.KoreanDictionaryProvider._
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TIOStreamTransport

/**
 * Create Korean Parsing goldenset from the goldenset resource that contains goldenset chunks.
 * The first argument is a gzipped output file.
 *
 * usage: ./pants goal run src/scala/com/twitter/penguin/korean/tools:create_parsing_goldenset
 * --jvm-run-args="/Users/hohyonryu/workspace/penguin-binaries/tests/com/twitter/penguin/korean/goldenset.txt.gz"
 */
object CreateParsingGoldenset {
  def main(args: Array[String]) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Please specify an output file")
    }

    System.err.println("Reading the goldenset..")

    val parsed = readFileByLineFromResources("goldenset.txt").flatMap {
      case line if line.length > 0 =>
        val chunk = line.trim
        val parsed = tokenizeWithNormalization(chunk)
        Some(ParseItem(chunk, parsed.map(p => KoreanTokenThrift(p.text, p.pos.id, p.unknown))))
      case line => None
    }.toSeq


    val outputFile: String = args(0)

    System.err.println("Writing the new goldenset to " + outputFile)

    val out = new GZIPOutputStream(new FileOutputStream(outputFile))
    val binaryOut = new TBinaryProtocol(new TIOStreamTransport(out))
    ParsingGoldenset(parsed).write(binaryOut)
    out.close()

    System.err.println("Testing the new goldenset " + outputFile)

    val input = readGzipTBininaryFromFile(outputFile)
    val loaded = ParsingGoldenset.decode(input).goldenset

    assert(loaded.equals(parsed))

    System.err.println("Updated goldenset in " + outputFile)
  }
}
