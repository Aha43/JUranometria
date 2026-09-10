# Overview projections, to the last decimal on one machine

Sprint 30, issue #296; classified in Sprint 31, issue #315.

**This is one machine's answer.** What is written here is what the desktop
this was generated on calls things and binds; another desktop answers differently
and is not wrong. The contract holds it to reproducing within an environment,
never across two - the portable half of this study is the document beside it.

| the machine | |
|---|---|
| operating system | Mac OS X |
| architecture | aarch64 |
| Java | 21.0.11 |
| look and feel | Metal |
| headless | true |

The residues below are rounding in double arithmetic: 7.8e-14 degrees on
one machine and 7.7e-14 on another, because a JDK and a chip are free to
associate a sum differently. The report beside this one says what the study
concluded from them - that each candidate agrees with what it was checked
against, and that a round trip returns where it started - which is the same on
any machine that can add.

| candidate | worst difference | round trip |
|---|---:|---:|
| gnomonic | 0.000e+00 | 7.7e-14° |
| stereographic | 2.111e-13 | 7.8e-14° |
| orthographic | 0.000e+00 | 3.3e-13° |
