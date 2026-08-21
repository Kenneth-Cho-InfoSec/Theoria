<!--
SPDX-FileCopyrightText: 2026 kennethcho
SPDX-License-Identifier: MPL-2.0
-->

# License and provenance boundaries

Theoria contains code from more than one provenance category. A file is not relicensed merely
because it has been edited.

## Inherited Apache code

Code inherited from `IacobIonut01/ReFra` remains under Apache License 2.0 unless its copyright
holders grant a different license. Its Apache header, attribution, and applicable notices must be
preserved.

## Independently authored code

New code authored for Theoria, and clean-room replacements independently implemented from
documented behavior, may use MPL 2.0 after provenance review. A replacement must not copy the
upstream implementation, structure, comments, or other expressive details merely to change its
license.

## Third-party and generated material

The following remain under their original terms and are excluded from the project MPL-2.0 set:

- vendored codec headers and native libraries, including libheif, libraw, libjpeg, libpng, zlib,
  and libtiff;
- external library modules such as cropper and any bundled upstream sources;
- Gradle wrapper files and generated build outputs;
- model binaries, screenshots, and other binary assets whose terms are supplied separately;
- LineageOS-derived code and any file carrying an explicit third-party copyright notice.

When provenance is uncertain, the more restrictive existing license and attribution are retained
until the source and copyright status have been verified.
