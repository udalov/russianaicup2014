russianaicup2014
================

My solution of Russian AI Cup 2014 -- CodeHockey (http://2014.russianaicup.ru/)

Results:
* Round 1 ([v11](030f33379a7c20ad8c7ca01460177f9ccdfdc3a3)) -- 171st place, 69% wins
* Round 2 ([v14](6582f99b5aaf3d91e4b3b7524b52984dc0b1d9ac)) -- 163th place, 57% wins
* Sandbox overall -- 50th place

To run the local-runner from the same process from IDEA you'll need to install [Kotlin](http://github.com/JetBrains/kotlin/) plugin.

To run the auto tester you'll need bash and Kotlin compiler.
Run `ant update` once to download the latest beta build of kotlinc,
`ant` to build an executable jar suitable for auto testing,
`ant src` to package the solution to a zip for the online judge.
