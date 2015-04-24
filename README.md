# els-toolkit
[![Build Status](https://travis-ci.org/PawelStroinski/els-toolkit.svg?branch=master)](https://travis-ci.org/PawelStroinski/els-toolkit)

Equidistant Letter Sequences Monte Carlo experiment

## Usage

    lein run

## Options

els-toolkit executes a protocol defined in an XML file:

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

    $ lein run

    Using 'protocol.xml' protocol file. (Different can be passed as an argument.)

    {:table
     {:area 36,
      :cylinder 9,
      :elses
      #{{:word "ORDER", :start 10, :skip 10}
        {:word "FOOD", :start 9, :skip 10}
        {:word "GARY", :start 27, :skip 10}},
      :h 6,
      :y 1,
      :x 0,
      :w 6},
     :same-or-better 137}

## License

Copyright © 2015 Paweł Stroiński

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.