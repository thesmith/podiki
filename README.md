podiki
======

There are hundreds of thousands of podcasts out there, each with hundreds of episodes chock full of really useful information and awesome songs. Problem is, that's all locked up behind a lifetime's worth of audio, unindexed and unsearchable.

[Podiki](http://github.com/thesmith/podiki) detects songs and transcribes speech in podcasts, making it available to be searched, linked up, indexed and updated.

There are two parts of Podiki: the processing of podcasts and a wiki.

Podcast Processing
----------------
http://github.com/thesmith/podiki

Submitted podcasts' new episodes are crawled and all the speech and song data extracted. As users correct the text this creates a feedback loop that updates the linguistic model used to transcribe future episodes.

The song information is determined using [EchoPrint](http://echoprint.me/) and the speech detection and transcription uses the [Sphinx4](http://cmusphinx.sourceforge.net/sphinx4/) library.

The background processing is written in [Scala](http://www.scala-lang.org/) and is backed by [Redis](http://redis.io) (atm).

Wiki
---
http://github.com/thesmith/podiki-web

The wiki lets users submit podcasts for processing and edit the songs and text and add additional links to things that are being talked about in the podcast.

The wiki web-app is also built in Scala using [Play](http://www.playframework.org/) and tracks are linked to using the [Spotify API](https://developer.spotify.com/technologies/web-api/search/).

TODO
-----

It is better to be done than anywhere near perfect. This is only just done.

Currently the wiki only allows the text and a few other bits to be edited and the feedback loop to the linguistic model isn't working.
