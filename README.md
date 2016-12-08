# els-toolkit
[![Build Status](https://travis-ci.org/PawelStroinski/els-toolkit.svg?branch=master)](https://travis-ci.org/PawelStroinski/els-toolkit)

Equidistant Letter Sequences Monte Carlo experiment

## Usage

### Alternative 1

Download the [els-toolkit-0.2.0.zip](https://github.com/PawelStroinski/els-toolkit/releases/download/0.2.0/els-toolkit-0.2.0.zip) file, extract it, and run the following command in the same directory:

    java -server -jar els-toolkit-0.2.0-standalone.jar
    
If your system does not recognize `java`, install [Java](https://java.com/en/download/) first (version 8 is fine).
    
For performance reasons, it is good to specify the amount of RAM to use (here is 8GB):

    java -Xmx8g -Xms8g -server -jar els-toolkit-0.2.0-standalone.jar

### Alternative 2

After checking out the code, run the following command in the same directory:

    lein run

If your system does not recognize `lein`, install [Leiningen](https://github.com/technomancy/leiningen/#installation) first.

### Alternative 3

If you wish to build the jar file rather than downloading it, see **Alternative 2** but use the following command instead:

    lein uberjar

Afterwards, use the command from **Alternative 1** to run the jar file built.

## Options

els-toolkit executes a protocol defined in an XML file such as the following:

    <protocol>
        <!-- Filename/URL of a text file. The file should contain letters/numbers
             only (no spaces etc.). -->
        <text>example-text.txt</text>

        <!-- Any number of words to look for. Individual words and sets of synonyms
             can be provided. -->
        <word>FOOD</word>
        <word>
            <synonym>ORDER</synonym>
            <synonym>REQUEST</synonym>
        </word>
        <word>GARY</word>

        <!-- Search for reversed words too. -->
        <reverse>false</reverse>

        <!-- Min & max skip numbers. '1' means the next letter, '2' means the letter
             after the next letter, etc. '1' is the lowest valid value. 'max' means
             the largest skip for text/word. -->
        <min-skip>1</min-skip>
        <max-skip>100</max-skip>

        <!-- Max cylinder size. '1' is the lowest valid value. 'max' means the
             largest cylinder size for ELSes. -->
        <max-cylinder>50</max-cylinder>

        <!-- Text population is a number of shuffled texts plus a single original
             text. -->
        <text-population>200</text-population>

        <!-- Shuffling used to generate text population ('letter-shuffling'
             or 'els-random-placement'). -->
        <shuffling>els-random-placement</shuffling>

        <!-- Seed of pseudo-random number generator. Changing this number will
             change the text population. Any non-zero value from -2147483648
             to 2147483647 (inclusive) is valid. -->
        <seed>8176753</seed>
    </protocol>

## Examples

    $ java -server -jar els-toolkit-0.2.0-standalone.jar

    Using 'protocol.xml' protocol file. (A different one can be passed in as an argument.)
    
    200/200   100% [==================================================]  ETA: 00:00
    
    {:table
     {:x 0,
      :w 6,
      :y 1,
      :h 6,
      :area 36,
      :elses
      #{{:word "ORDER", :start 10, :skip 10}
        {:word "FOOD", :start 9, :skip 10}
        {:word "GARY", :start 27, :skip 10}},
      :cylinder 9},
     :same-or-better 137}

## License

Copyright © 2015, 2016 Paweł Stroiński

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.