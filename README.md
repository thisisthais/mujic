mujic will be a clojure program that uses a Markov chain to generate midi music.

It will consist of three parts:

- Parser
  Parses a midi file, groups notes as chords based on duration.
- Markov chain
  Builds a probability transition matrix based on the parsed file.
- Generator
  Makes a new midi file using the Markov chain.
