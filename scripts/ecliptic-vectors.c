/* Generates reference vectors for JUranometria's ecliptic reference
   geometry.  Uses routines provided by SOFA under licence; this file
   is not SOFA software and is not endorsed by SOFA.  It calls the
   unmodified library to print

     (a) the J2000 mean obliquity and the equatorial (ICRS/J2000)
         directions of chosen ecliptic longitudes, so the atlas's
         fixed mean-ecliptic-of-J2000 model can be checked against an
         authority; and

     (b) the rival candidate - the MEAN ecliptic and equinox OF DATE -
         expressed in the same ICRS/J2000 frame, so the two candidates
         can be compared without routing either of them through the
         atlas's own transformation.

   Both candidates here are MEAN: no nutation is applied to either.
   That matters, because the atlas's own toJ2000() expects a TRUE of
   date direction and removes nutation first; feeding it a mean one
   adds a rotation that belongs to neither candidate (PR #276 review).
   Everything below comes from SOFA's own matrices instead. */
#include <stdio.h>
#include "sofa.h"
#include "sofam.h"

/* The ICRS direction of an ecliptic-frame axis, for the mean ecliptic
   and equinox of the given date.  Axis 0 is the equinox (the ecliptic
   x-axis); axis 2 is the ecliptic north pole. */
static void ecliptic_axis_vector(double d1, double d2, int axis,
                                 double eq[3]) {
    double rm[3][3], v[3] = {0.0, 0.0, 0.0};
    v[axis] = 1.0;
    iauEcm06(d1, d2, rm);   /* ICRS -> mean ecliptic & equinox of date */
    iauTrxp(rm, v, eq);     /* transpose: ecliptic frame -> ICRS       */
}

static void ecliptic_axis(double d1, double d2, int axis,
                          double *ra, double *dec) {
    double eq[3];
    ecliptic_axis_vector(d1, d2, axis, eq);
    iauC2s(eq, ra, dec);
    *ra = iauAnp(*ra) * DR2D;
    *dec = *dec * DR2D;
}

/* SOFA's iauEcm06 and iauObl06 take TT.  The dates below are stated
   in UTC and passed through unconverted.

   This is a SENSITIVITY EXPERIMENT, not a conversion.  It moves the
   date by a fixed number of seconds and measures how far the two
   ecliptic axes travel.  It is deliberately not called the cost of
   omitting UTC->TT: UTC did not exist in 1900, and TAI-UTC after
   today depends on leap-second decisions nobody has taken yet, so no
   single offset is the conversion for all five dates (PR #276 round
   3).

   Two shifts are reported.  69.184 s is today's actual TT-UTC
   (32.184 + 37 leap seconds), which makes the experiment concrete.
   300 s is an ALLOWANCE chosen to sit above delta-T across
   1900-2100: it was near zero around 1900, is about 69 s now, and
   published projections for 2100 are of order 200 s with wide
   uncertainty.  No maximum beyond 300 s is claimed, and the atlas
   ships no delta-T table.

   The larger displacement of the two axes is returned, in
   arcseconds. */
static double shift_arcsec(double d1, double d2, double seconds) {
    double a[3], b[3], worst = 0.0, apart;
    double shifted = d2 + seconds / 86400.0;
    for (int axis = 0; axis < 3; axis += 2) {   /* equinox, then pole */
        ecliptic_axis_vector(d1, d2, axis, a);
        ecliptic_axis_vector(d1, shifted, axis, b);
        apart = iauSepp(a, b) * DR2D * 3600.0;
        if (apart > worst) {
            worst = apart;
        }
    }
    return worst;
}

#define SHIFT_TODAY 69.184
#define SHIFT_ALLOWANCE 300.0

static double worst_today = 0.0;
static double worst_allowance = 0.0;

static void candidate(const char *iso, int y, int m, int d,
                      int hh, int mm, int ss) {
    double djm0, djm, frac, d2, eps, pra, pdec, era, edec;
    iauCal2jd(y, m, d, &djm0, &djm);
    frac = (hh * 3600.0 + mm * 60.0 + ss) / 86400.0;
    d2 = djm + frac;

    eps = iauObl06(djm0, d2) * DR2D;         /* mean obliquity of date */
    ecliptic_axis(djm0, d2, 2, &pra, &pdec); /* ecliptic pole of date  */
    ecliptic_axis(djm0, d2, 0, &era, &edec); /* mean equinox of date   */

    {
        double today = shift_arcsec(djm0, d2, SHIFT_TODAY);
        double allowance = shift_arcsec(djm0, d2, SHIFT_ALLOWANCE);
        if (today > worst_today) {
            worst_today = today;
        }
        if (allowance > worst_allowance) {
            worst_allowance = allowance;
        }
    }

    printf("ofdate %s %.9f %.9f %.9f %.9f %.9f\n",
           iso, eps, pra, pdec, era, edec);
}

int main(void) {
    double djm0, djm, rm[3][3], v[3], eq[3], ra, dec, eps;
    iauCal2jd(2000, 1, 1, &djm0, &djm);

    eps = iauObl80(djm0, djm + 0.5);        /* IAU 1980 mean obliquity  */

    /* The whole provenance header is emitted here rather than added
       to the file by hand, so the fixture is reproducible from this
       program alone: regenerating it can no longer silently drop the
       terms the licence asks for. */
    printf("# Reference vectors for the ecliptic reference geometry\n");
    printf("#\n");
    printf("# Provenance\n");
    printf("#   Computed with IAU SOFA, release 2023-10-11, ANSI C:\n");
    printf("#     https://www.iausofa.org/2023-10-11c\n");
    printf("#     https://www.iausofa.org/s/sofa_c-20231011tar.gz\n");
    printf("#     sha256 d9c10833cae8b4d9361a0ffda31ec361fd1262362025bec4d4e51a880150ace2\n");
    printf("#   Generated by scripts/ecliptic-vectors.c in this repository, which\n");
    printf("#   calls the unmodified library (iauObl80, iauObl06, iauEcm06). This\n");
    printf("#   file and that program are not SOFA software and are not\n");
    printf("#   endorsed by, SOFA.\n");
    printf("#\n");
    printf("#   The atlas takes no dependency on SOFA at run time or at build\n");
    printf("#   time: these are numbers an authority produced, checked in so the\n");
    printf("#   fixed mean-ecliptic-of-J2000 model can be held to them, and\n");
    printf("#   nothing here is fetched, compiled, or called by the application.\n");
    printf("#\n");
    printf("# Columns\n");
    printf("#   obliquity_j2000_deg : IAU 1980 mean obliquity at J2000.0\n");
    printf("#   ecl <lambda> <beta> <ra_j2000> <dec_j2000> : the ICRS/J2000\n");
    printf("#     equatorial direction of an ecliptic-frame direction, via the\n");
    printf("#     J2000 ecliptic matrix (iauEcm06), degrees.\n");
    printf("#   eclpole <ra_j2000> <dec_j2000> : the J2000 ecliptic north pole.\n");
    printf("#   timescale_shift_arcsec <seconds> <worst> : a SENSITIVITY\n");
    printf("#     experiment, not a conversion. Moving every date below by\n");
    printf("#     <seconds> displaces the ecliptic axes by at most <worst>\n");
    printf("#     arcseconds. The dates are UTC and are passed to SOFA's\n");
    printf("#     TT-based routines unconverted; no single offset is the\n");
    printf("#     true conversion for all of them, because UTC did not exist\n");
    printf("#     in 1900 and future leap seconds are undecided. 69.184 s is\n");
    printf("#     today's TT-UTC; 300 s is an allowance above delta-T across\n");
    printf("#     1900-2100, beyond which no maximum is claimed.\n");
    printf("#   ofdate <iso> <eps_of_date> <pole_ra> <pole_dec> <equinox_ra>\n");
    printf("#     <equinox_dec> : the rival candidate - the MEAN ecliptic and\n");
    printf("#     equinox OF DATE - expressed in the same ICRS/J2000 frame.\n");
    printf("#     Mean, not true: no nutation is applied, to either candidate.\n");
    printf("#\n");
    printf("obliquity_j2000_deg %.9f\n", eps * DR2D);

    /* ICRS -> ecliptic (of date) rotation; at J2000 this is the J2000
       ecliptic frame including frame bias. Transpose gives ecliptic
       -> ICRS, so an ecliptic direction becomes an equatorial one. */
    iauEcm06(djm0, djm + 0.5, rm);
    printf("# lambda_deg  beta_deg  ra_j2000_deg  dec_j2000_deg\n");
    for (int i = 0; i < 8; i++) {
        double lambda = i * 45.0;
        iauS2c(lambda * DD2R, 0.0, v);      /* ecliptic unit vector */
        iauTrxp(rm, v, eq);                 /* ecliptic -> ICRS      */
        iauC2s(eq, &ra, &dec);
        printf("ecl %.4f %.4f %.9f %.9f\n", lambda, 0.0,
               iauAnp(ra) * DR2D, dec * DR2D);
    }

    /* The J2000 candidate's own pole, in ICRS: the reference both
       candidates are measured against. */
    ecliptic_axis(djm0, djm + 0.5, 2, &ra, &dec);
    printf("# ra_j2000_deg  dec_j2000_deg\n");
    printf("eclpole %.9f %.9f\n", ra, dec);

    /* The rival candidate at five dates: the MEAN ecliptic and
       equinox OF DATE, expressed in ICRS/J2000.
       iso  eps_of_date_deg  pole_ra pole_dec  equinox_ra equinox_dec */
    printf("# iso  eps_of_date_deg  pole_ra  pole_dec  equinox_ra  equinox_dec\n");
    candidate("1900-01-01T00:00:00Z", 1900, 1, 1, 0, 0, 0);
    candidate("2000-01-01T12:00:00Z", 2000, 1, 1, 12, 0, 0);
    candidate("2026-03-20T21:33:00Z", 2026, 3, 20, 21, 33, 0);
    candidate("2050-07-04T03:00:00Z", 2050, 7, 4, 3, 0, 0);
    candidate("2100-01-01T00:00:00Z", 2100, 1, 1, 0, 0, 0);
    printf("timescale_shift_arcsec %.3f %.9f\n",
           SHIFT_TODAY, worst_today);
    printf("timescale_shift_arcsec %.3f %.9f\n",
           SHIFT_ALLOWANCE, worst_allowance);
    return 0;
}
