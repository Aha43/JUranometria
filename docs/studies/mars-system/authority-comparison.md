# #601 Mars authority comparison

Skyfield on the pinned kernels against the kept JPL Horizons responses (Horizons: `mar099` for Mars, Phobos and Deimos, DE441 for the Earth). The Earth and the Sun are DE440's (`de440s.bsp`). Observers at sea level on the WGS84 ellipsoid. Civil time: UT1 before 1972, UTC from 1972; the record's expiry, 2027-06-28, read from `Leap_Second.dat`, bounds the exact era.

Two Mars models: **barycentre**, DE440's Mars barycentre alone (0 -> 4), every row 1900-2100; **centre**, the barycentre plus `mar099s`'s 4 -> 499, only rows inside `mar099s`'s span (JD 2449718.5 to 2469807.5, one day kept clear of each end).

Constants read from `pck00011.tpc`: Mars's radii 3396.19, 3396.19, 3376.2 km; pole RA 317.269202 -0.10927547 T, Dec 54.432516 -0.05827105 T, with 5 periodic terms in RA and 5 in Dec over 26 angles; Phobos's radii [13.0, 11.4, 9.1], Deimos's [7.8, 6.0, 5.1] km; the Sun's radius 695700 km.

## Mars, barycentre model: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (6475 rows) | 1962-1971 (1049 rows) | 1972-2027-06 (6540 rows) | after (7972 rows) | worst row |
|---|---:|---:|---:|---:|---|
| apparent, Sun deflection, beyond 1° from the Sun″ | +0.2452 | +0.0619 | +0.0536 | +0.1943 | matrix-7d-mars-geocentric 1900-04-23 00:00 |
| apparent, Sun deflection″ | +0.2452 | +0.0619 | +0.0536 | +1.2383 | matrix-7d-mars-geocentric 2038-11-01 00:00 |
| apparent, no deflection″ | +0.6950 | +0.4343 | +0.6936 | +0.8374 | matrix-7d-mars-oslo 2055-11-29 00:00 |
| astrometric″ | +0.0010 | +0.0194 | +0.0001 | +0.0208 | matrix-7d-mars-oslo 2097-07-29 00:00 |
| diameter″ | -0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-mars-oslo 2097-09-09 00:00 |
| distance km | +0.4846 | +9.9341 | +0.0563 | -9.0995 | matrix-7d-mars-geocentric 1965-01-04 00:00 |
| horizontal″ | +0.9670 | +2.1113 | +2.6813 | +409.1583 | matrix-7d-mars-oslo 2100-09-20 00:00 |
| illuminated % | +0.0033 | -0.0032 | +0.0033 | +0.0033 | matrix-7d-mars-oslo 2084-07-17 00:00 |
| illuminated, Sun light time and aberration % | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-mars-oslo 1965-02-01 00:00 |
| phase angle vs phi″ | +18.3974 | +18.3831 | +18.4044 | -18.3966 | matrix-7d-mars-geocentric 1982-12-20 00:00 |
| phase angle, Sun light time and aberration, vs phi″ | +0.1800 | +0.1863 | +0.1800 | +0.1835 | matrix-7d-mars-oslo 1967-04-03 00:00 |
| phase angle, Sun light time and aberration″ | +21.1449 | +20.9210 | +21.0205 | +21.1023 | matrix-7d-mars-oslo 1944-11-20 00:00 |
| phase angle″ | +37.8998 | -36.1798 | -37.6637 | -37.9187 | matrix-7d-mars-oslo 2084-11-06 00:00 |
| pole Dec, secular only″ | +5562.2252 | +5563.4064 | +5570.1023 | +5578.6397 | matrix-7d-mars-geocentric 2099-12-28 00:00 |
| pole Dec, with periodic terms″ | -0.0180 | -0.0180 | +0.0180 | +0.0180 | matrix-7d-mars-oslo 1933-02-20 00:00 |
| pole RA, secular only″ | -1483.0695 | -1483.3402 | -1484.6816 | -1486.4034 | matrix-7d-mars-geocentric 2099-05-18 00:00 |
| pole RA, with periodic terms″ | -0.0180 | -0.0180 | +0.0180 | +0.0180 | matrix-7d-mars-oslo 1984-05-21 00:00 |
| pole angle, secular only″ | -5919.9643 | -5903.9852 | -5927.5711 | -5930.8132 | matrix-7d-mars-geocentric 2031-07-21 00:00 |
| pole angle, with periodic terms″ | -0.2478 | +0.1879 | -0.1815 | +0.2770 | matrix-7d-mars-geocentric 2099-02-23 00:00 |
| sub-observer latitude, centric vs Horizons″ | +968.7872 | +968.4898 | +969.1638 | +969.5500 | matrix-7d-mars-geocentric 2066-01-04 00:00 |
| sub-observer latitude, detic vs Horizons″ | +0.0022 | +0.0097 | -0.0018 | +0.0129 | matrix-7d-mars-oslo 2099-09-28 00:00 |

## Mars, centre model: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (0 rows) | 1962-1971 (0 rows) | 1972-2027-06 (4130 rows) | after (2495 rows) | worst row |
|---|---:|---:|---:|---:|---|
| apparent, Sun deflection, beyond 1° from the Sun″ | - | - | +0.0536 | +0.0521 | matrix-7d-mars-geocentric 1998-03-09 00:00 |
| apparent, Sun deflection″ | - | - | +0.0536 | +1.2383 | matrix-7d-mars-geocentric 2038-11-01 00:00 |
| apparent, no deflection″ | - | - | +0.6936 | +0.7288 | matrix-7d-mars-oslo 2045-05-01 00:00 |
| astrometric″ | - | - | +0.0001 | +0.0012 | matrix-7d-mars-oslo 2048-06-01 00:00 |
| diameter″ | - | - | +0.0000 | +0.0000 | opposition-2012-mars-oslo-hourly 2012-03-03 05:00 |
| distance km | - | - | +0.0563 | +0.4414 | matrix-7d-mars-oslo 2049-12-27 00:00 |
| horizontal″ | - | - | +2.6813 | +32.2147 | matrix-7d-mars-oslo 2049-11-08 00:00 |
| illuminated % | - | - | +0.0033 | +0.0032 | matrix-7d-mars-oslo 2005-07-18 00:00 |
| illuminated, Sun light time and aberration % | - | - | +0.0000 | +0.0000 | matrix-7d-mars-oslo 2028-12-04 00:00 |
| phase angle vs phi″ | - | - | -18.3992 | -18.3966 | matrix-7d-mars-geocentric 2009-04-27 00:00 |
| phase angle, Sun light time and aberration, vs phi″ | - | - | +0.1800 | -0.1799 | matrix-7d-mars-oslo 2001-06-04 00:00 |
| phase angle, Sun light time and aberration″ | - | - | +20.9746 | +21.0334 | matrix-7d-mars-oslo 2038-11-08 00:00 |
| phase angle″ | - | - | -37.6637 | -37.7038 | matrix-7d-mars-oslo 2037-11-16 00:00 |

## Phobos: worst absolute difference from Horizons, by era (inside `mar099s`)

| quantity | 1900-1961 (0 rows) | 1962-1971 (0 rows) | 1972-2027-06 (4272 rows) | after (2637 rows) | worst row |
|---|---:|---:|---:|---:|---|
| X apparent″ | - | - | -0.0010 | -0.0022 | matrix-7d-phobos-geocentric 2038-10-31 00:00 |
| Y apparent″ | - | - | +0.0013 | +0.0020 | matrix-7d-phobos-geocentric 2030-05-26 00:00 |
| apparent, beyond 1° from the Sun″ | - | - | +0.0536 | +0.0522 | matrix-7d-phobos-geocentric 1998-03-08 00:00 |
| apparent″ | - | - | +1.0543 | +0.0522 | matrix-7d-phobos-oslo 2023-11-19 00:00 |
| astrometric″ | - | - | +0.0001 | +0.0012 | matrix-7d-phobos-oslo 2048-05-31 00:00 |
| diameter, largest radius″ | - | - | -0.0000 | +0.0000 | matrix-7d-phobos-geocentric 2009-03-29 00:00 |
| distance km | - | - | +0.0563 | +0.4404 | matrix-7d-phobos-oslo 2049-12-26 00:00 |
| horizontal″ | - | - | +2.6910 | +32.2158 | matrix-7d-phobos-oslo 2049-11-07 00:00 |
| position angle, beyond 20″ of Mars° | - | - | -0.0027 | +0.0028 | matrix-7d-phobos-geocentric 2033-07-03 00:00 |
| position angle, within 20″ of Mars° | - | - | -0.0560 | -0.0336 | matrix-7d-phobos-geocentric 2015-06-14 00:00 |
| position angle° | - | - | -0.0560 | -0.0336 | matrix-7d-phobos-geocentric 2015-06-14 00:00 |
| separation″ | - | - | -0.0019 | -0.0023 | matrix-7d-phobos-geocentric 2030-05-26 00:00 |

## Deimos: worst absolute difference from Horizons, by era (inside `mar099s`)

| quantity | 1900-1961 (0 rows) | 1962-1971 (0 rows) | 1972-2027-06 (4272 rows) | after (2637 rows) | worst row |
|---|---:|---:|---:|---:|---|
| X apparent″ | - | - | -0.0021 | -0.0024 | matrix-7d-deimos-oslo 2038-10-31 00:00 |
| Y apparent″ | - | - | -0.0095 | +0.0021 | matrix-7d-deimos-oslo 2023-11-19 00:00 |
| apparent, beyond 1° from the Sun″ | - | - | +0.0536 | +0.0522 | matrix-7d-deimos-geocentric 1998-03-08 00:00 |
| apparent″ | - | - | +1.0581 | +0.0522 | matrix-7d-deimos-oslo 2023-11-19 00:00 |
| astrometric″ | - | - | +0.0001 | +0.0012 | matrix-7d-deimos-oslo 2048-05-31 00:00 |
| diameter, largest radius″ | - | - | +0.0000 | +0.0000 | opposition-2003-deimos-geocentric-10m 2003-08-28 16:50 |
| distance km | - | - | +0.0563 | +0.4404 | matrix-7d-deimos-oslo 2049-12-26 00:00 |
| horizontal″ | - | - | +2.6910 | +32.2158 | matrix-7d-deimos-oslo 2049-11-07 00:00 |
| position angle, beyond 20″ of Mars° | - | - | -0.0060 | -0.0060 | matrix-7d-deimos-geocentric 2033-06-26 00:00 |
| position angle, within 20″ of Mars° | - | - | +0.0536 | -0.0140 | matrix-7d-deimos-oslo 2023-11-19 00:00 |
| position angle° | - | - | +0.0536 | -0.0140 | matrix-7d-deimos-oslo 2023-11-19 00:00 |
| separation″ | - | - | -0.0047 | -0.0064 | matrix-7d-deimos-geocentric 2030-05-26 00:00 |

## Visibility states against Horizons' codes

Ours on Horizons' definition: limb to limb on Mars's equatorial sphere and each moon's largest PCK radius, light time considered; the shadow at the moon's emission epoch, in Mars's equatorial-sphere umbra. Horizons' codes: `t` transiting, `O` occulted, `p`/`u` partial/total umbral eclipse, `P`/`U` occulted while partially/totally eclipsed, `*` clear.

| moon | agreeing | disagreeing | Horizons' codes |
|---|---:|---:|---|
| phobos | 6,905 | 4 | `*` 5891, `O` 251, `P` 2, `U` 142, `t` 415, `u` 208 |
| deimos | 6,909 | 0 | `*` 6769, `O` 44, `U` 6, `t` 54, `u` 36 |

Disagreements, phobos (4):
- matrix-7d-phobos-oslo 2028-12-10 00:00: ours p, Horizons * (separation 5.585″, Horizons 5.585″)
- matrix-7d-phobos-oslo 2034-01-08 00:00: ours p, Horizons u (separation 5.740″, Horizons 5.740″)
- matrix-7d-phobos-geocentric 2028-12-10 00:00: ours p, Horizons * (separation 5.585″, Horizons 5.585″)
- matrix-7d-phobos-geocentric 2034-01-08 00:00: ours p, Horizons u (separation 5.740″, Horizons 5.740″)

## The planetary ephemeris, and the centre offset

| quantity | worst |
|---|---:|
| barycentre km | 4.59524 |
| earth km | 0.494644 |
| geocentric direction″ | 0.0141205 |
| geocentric distance km | 2.90128 |
| centre vs barycentre, geocentric direction, 1995-2050 every 6 h″ | 7.49617e-07 |
