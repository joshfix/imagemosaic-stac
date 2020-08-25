package com.joshfix.stac.store.mosaic;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.parameter.Parameter;
import org.opengis.parameter.GeneralParameterValue;

public class RequestTypeHelper {
    public static RequestType determineRequestType(GeneralParameterValue[] parameters, double minX, double maxY) {
        for (GeneralParameterValue paramValue : parameters) {
            if (StacMosaicFormat.READ_GRIDGEOMETRY2D.getName().getCode().equals(
                    paramValue.getDescriptor().getName().getCode())) {

                GridGeometry2D gridGeometry2D = (GridGeometry2D) ((Parameter) paramValue).getValue();
                GridEnvelope2D gridRange2D = gridGeometry2D.getGridRange2D();

                long roundedMinX = Math.round(minX);
                long roundedMaxY = Math.round(maxY);
                long roundedCoord0 = Math.round(gridGeometry2D.getEnvelope().getLowerCorner().getCoordinate()[0]);
                long roundedCoord1 = Math.round(gridGeometry2D.getEnvelope().getUpperCorner().getCoordinate()[1]);

                if (gridRange2D.getMinX() == 0
                        && gridRange2D.getMinY() == 0
                        && gridRange2D.getMaxX() == 5
                        && gridRange2D.getMaxY() == 5
                        && roundedCoord0 == roundedMinX
                        && roundedCoord1 == roundedMaxY) {
                    return RequestType.SAMPLE_5X5;
                } else if (gridRange2D.getMinX() == 0
                        && gridRange2D.getMinY() == 0
                        && gridRange2D.getMaxX() == 6
                        && gridRange2D.getMaxY() == 3
                        && roundedCoord0 == roundedMinX
                        && roundedCoord1 == roundedMaxY) {
                    return RequestType.SAMPLE_6X3;
                } else if (gridRange2D.getMinX() == 0
                        && gridRange2D.getMinY() == 0
                        && gridRange2D.getMaxX() == 10
                        && gridRange2D.getMaxY() == 10
                        && roundedCoord0 == roundedMinX
                        && roundedCoord1 == roundedMaxY) {
                    return RequestType.SAMPLE_10X10;
                } else {
                    return RequestType.STANDARD;
                }
            }
        }
        return RequestType.STANDARD;
    }
}
