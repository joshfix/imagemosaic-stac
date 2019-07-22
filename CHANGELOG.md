# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
## [0.1.0] - 2019-06-26
### Added
- Added helper class to build feature info directly from STAC item properties classes vs attempting to determine 
the type from the value of the property.
- Added filter to ensure that all items from a STAC search are within the declaired lat lon bbox AOI if the bbox 
was not used to execute the STAC search.
- Added code to cache a small section of the sample/default item coverage in StacMosaicReader and return it early 
when GeoServer is probing for band metadata, etc.
- Added GeoServer catalog event listener to update grids on CoverageInfo objects
- Added AOI layer filter

### Changed
- Fixed NPE where new store creation was attempting to use the request bbox but none was present
- Changed how the max pixel resolution is used.  Instead of using custom values provided by the user, the values are 
read directly from the sample image.
- StacMosaicReader modified to use global bounds for it's originalEnvelope instead of the sample image.
- Modified StacVectorFeatureSource to not cache results and submit a fresh query with every request
- Changed StacMosaicReader to always calculate original grid range using global bounds and the resolution obtained from 
the sample item
- No longer use custom method to calculate world to grid transform in StacMosaicReader.  Instead rely on the super class 
implementation

### Removed
- Removed ability to customize grid height/width