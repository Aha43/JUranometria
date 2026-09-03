/* Generates reference vectors for JUranometria's sky-orientation
   model.  Uses routines provided by SOFA under licence; this file is
   not SOFA software and is not endorsed by SOFA.  It calls the
   unmodified library to print the true-equator-and-equinox-of-date to
   J2000 rotation applied to chosen directions. */
#include <stdio.h>
#include <string.h>
#include "sofa.h"
#include "sofam.h"

static void row(const char *utc, int y, int mo, int d, int h, int mi, double s,
                double ra_deg, double dec_deg) {
    double djm0, djm, jd, dpsi, deps, epsa, rp[3][3], rn[3][3], rall[3][3];
    double v[3], out[3], ra, dec, gmst;
    iauCal2jd(y, mo, d, &djm0, &djm);
    jd = djm0 + djm + (h + mi / 60.0 + s / 3600.0) / 24.0;

    /* The model under test treats UTC as both UT1 and TT; the vectors
       are generated the same way so the comparison isolates the
       rotation rather than the time scales. */
    iauPmat76(jd, 0.0, rp);            /* J2000 -> mean of date  */
    iauNut80(jd, 0.0, &dpsi, &deps);
    epsa = iauObl80(jd, 0.0);
    iauNumat(epsa, dpsi, deps, rn);    /* mean of date -> true   */
    iauRxr(rn, rp, rall);              /* J2000 -> true of date  */
    iauTr(rall, rall);                 /* true of date -> J2000  */

    iauS2c(ra_deg * DD2R, dec_deg * DD2R, v);
    iauRxp(rall, v, out);
    iauC2s(out, &ra, &dec);
    gmst = iauAnp(iauGmst82(jd, 0.0)) * DR2D;

    printf("%s %.6f %.6f %.9f %.9f %.9f\n", utc, ra_deg, dec_deg,
           iauAnp(ra) * DR2D, dec * DR2D, gmst);
}

int main(void) {
    printf("# utc ra_of_date dec_of_date ra_j2000 dec_j2000 gmst_deg\n");
    struct { const char *s; int y, mo, d, h, mi; double sec; } when[] = {
        {"2000-01-01T12:00:00Z", 2000, 1, 1, 12, 0, 0},
        {"1975-11-30T23:59:00Z", 1975, 11, 30, 23, 59, 0},
        {"1987-04-10T00:00:00Z", 1987, 4, 10, 0, 0, 0},
        {"2026-03-20T21:33:00Z", 2026, 3, 20, 21, 33, 0},
        {"2026-06-21T08:24:00Z", 2026, 6, 21, 8, 24, 0},
        {"2026-12-21T20:03:00Z", 2026, 12, 21, 20, 3, 0},
        {"2050-07-04T03:00:00Z", 2050, 7, 4, 3, 0, 0},
        {"2100-01-01T00:00:00Z", 2100, 1, 1, 0, 0, 0},
    };
    struct { double ra, dec; } where[] = {
        {0.0, 0.0}, {0.0, 90.0}, {0.0, -90.0}, {180.0, 0.0},
        {359.9, -0.1}, {83.822083, -5.391111}, {10.684708, 41.268750},
        {266.404996, -28.936172}, {90.0, 66.56}, {270.0, -66.56},
    };
    for (unsigned i = 0; i < sizeof when / sizeof when[0]; i++)
        for (unsigned j = 0; j < sizeof where / sizeof where[0]; j++)
            row(when[i].s, when[i].y, when[i].mo, when[i].d, when[i].h,
                when[i].mi, when[i].sec, where[j].ra, where[j].dec);
    return 0;
}
