![Grabble logo](https://user-images.githubusercontent.com/17273782/123671077-cb438c00-d835-11eb-9110-c9e6f26073ad.png)

This is an android game created by me as a part of my Bachelor's degree in computer science at the University of Edinburgh. One of the first complete projects I made by myself, as well as my first foray into android development, it has great sentimental value to me.

This repo serves to commemorate that, mostly so I can fondly look back at it; but perhaps an errant developer stumbling upon this in the future could steal some ideas from it.

## Details
This is a game about walking around the area near to the main University of Edinburgh campus, and collecting "letters" that spawn in different locations. (Our professor was inspired by Pokemon Go.) These letters are then used to form words from a predefined dictionary. The coursework specs for this project mandated those requirements, as well as the use of the name "grabble".

Because I'd be deeply unsatisfied with a result that is so barebones, my rendition of the project includes, as optional features I implemented because I alone wanted to do so:

* Narrative inspired by Roger Zelazny's "A Night in the Lonesome October" and motivating and tying into the base mechanics, featuring two competing teams: the Openers, seeking to complete their *Codex Maleficarum* and destroy the world; and the Closers, wishing to find all words for their *Sacred Tome* and preserve the world.
* A currency system allowing collected letters to be transformed to "Ash", and then repurposed to create other letters the player wants or needs.
* A progression system with (algorithmically-defined) experience collection and level-up mechanics, with 100 distinct ranks to achieve, allowing a Closer to ascend from being a Novice to the rank of Eternal Prophet; and Openers, conversely, to progress from a lowly Acolyte to assuming the role of the Dark Messiah.
* Persistent on-device storage of player progress.


### More information
If interested, have a peek at the PDF folder. The game is likely unplayable these days (by virtue of not having a placemark list to download), but I imagine perusing the code might be an amusing exercise. `XPUtils` would be a recommended starting point, as it contains most of the interesting tricks that were pleasurable to implement.


### Skills
I'll put this here just because it's not documented anywhere else. Every game needs to have a manual, even dead ones.

Levelling up also improves sight range, grab range, inventory space, and the "market conversion rates" for letters, for both teams.

#### Closers
Closers gain skills that improve their sight range and collection radius, incentivising and expediting exploration and letter collection.

* **Oracle** (Ⱚ, unlocks at level 5, "Accepted"): Sight radius increases linearly (+10% at level 5, +50% at level 100).
* **Keeper's Grace** (Ⱔ, unlocks at level 25, "Keeper"): Burning a letter at the Crematorium has a chance of generating 1 additional Ash (0.4% per level).
* **Sacred Will** (Ⱋ, unlocks at level 50, "Chosen"): Oracle now increases grab range as well.
* **Commanding Presence** (Ⱉ, unlocks at level 75, "Centurion"): Oracle benefits are doubled; every time a letter is collected, there is a minute chance an additional copy of it will be added to the inventory (0.05% per level).

#### Openers
Openers, on the other hand, gain skills that incentivise a heavier use of the Ashery and Crematorium (the "market") to allow for a more efficient and reckless use of resources.

* **Ashen Soul** (Ⱗ, unlocks at level 5, "Grunt"): Burning a letter at the Crematorium generates 1 additional Ash.
* **Dustbeckon** (Ⱑ, unlocks at level 25, "Reaver"): Permanently reduces the amount of letters needed to gain an extra Ash by 1.
* **Spire Agents** (Ⱙ, unlocks at level 50, "Chosen"): Creating letters at the Ashery is cheaper, increasing linearly with level (20% discount at level 50, 25% discount at level 100)
* **Toll the Spire** (Ⱘ, unlocks at level 75, "Spire Lord"): There is a chance that creating a Letter at the Ashery will cost you no Ash (0.1% per level).