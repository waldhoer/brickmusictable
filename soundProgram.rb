live_loop :midi_piano do
  
  note, duration, channel, volume, rotation = sync "/osc:192.168.0.57:50189/midi"
  factor = 0.5 * volume
  
  # Select instrument to play
  if channel == 0
    use_synth :piano
  elsif channel == 1
    use_synth :beep
  elsif channel == 2
    use_synth :kalimba
    factor = factor * 2
  elsif channel == 3
    use_synth :saw
    factor = factor*0.4 # Some samples require additional volume adjustments
  elsif channel == 4
    use_synth :dull_bell
  elsif channel == 5
    use_synth :blade
  elsif channel == 6
    use_synth :dull_bell
  elsif channel == 7
    use_synth :dpulse
  elsif channel == 8
    use_synth :chiplead
  elsif channel == 9
    use_synth :sc808_cowbell
  elsif channel == 10
    use_synth :zawa
  else
    use_synth :dark_ambience
  end
  
  
  # Normal instrument sounds
  if channel != 55
    # Perform rotation FX if set
    if rotation != 0
      with_fx :wobble do
        play note: note, release: duration * 0.001, mix: 1-1.0/rotation, amp: factor
      end
      # Default sound play
    else
      play note: note, release: duration * 0.001, amp: factor
    end
    
    # Click beats
  else
    use_synth :sc808_clap
    play 40, amp: volume*1.3
  end
end