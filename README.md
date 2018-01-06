mujic will be a clojure program that uses a Markov chain to generate midi music.

It will consist of three parts:

- Parser
  Parses a midi file, groups notes as chords based on duration. Here's a sample of the parsed midi file so far:
  [Chunk of parsed midi file](images/sample_parsed_midi.png)
  Ticks are related to the timing of the command (ticks per quarter note). Channel is equivalent to the instrument. Channel 0 is acoustic piano. Velocity is how hard/fast the note is struck. Velocity 0 is used for note-off events.
- Markov chain
  Builds a probability transition matrix based on the parsed file.
- Generator
  Makes a new midi file using the Markov chain.
