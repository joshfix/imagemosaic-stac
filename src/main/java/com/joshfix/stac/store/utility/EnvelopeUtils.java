package com.joshfix.stac.store.utility;

import org.geotools.gce.imagemosaic.Utils;
import org.locationtech.jts.geom.Envelope;

import java.math.BigDecimal;

/**
 * @author joshfix
 * Created on 2019-07-19
 */
public class EnvelopeUtils {

    public static Envelope buildEnvelope(Utils.BBOXFilterExtractor bboxExtractor) {
        double[] envelope = new double[]{
                Math.max(bboxExtractor.getBBox().getMinX(), -180.0),
                Math.max(bboxExtractor.getBBox().getMinY(), -90.0),
                Math.min(bboxExtractor.getBBox().getMaxX(), 180.0),
                Math.min(bboxExtractor.getBBox().getMaxY(), 90.0)
        };
        return buildEnvelope(envelope);
    }


    public static Envelope buildEnvelope(String envelope) {
        String[] stringCoords = envelope.split(",");
        double[] coords = new double[]{
                Double.valueOf(stringCoords[0]),
                Double.valueOf(stringCoords[1]),
                Double.valueOf(stringCoords[2]),
                Double.valueOf(stringCoords[3])
        };
        return buildEnvelope(coords);
    }

    public static Envelope buildEnvelope(double[] bbox) {
        return new Envelope(bbox[0],bbox[2], bbox[1], bbox[3]);
    }

    public static Envelope buildEnvelope(BigDecimal[] bbox) {
        if (bbox.length != 4) {
            throw new RuntimeException("Attempting to build a 4 coordinate bbox from an array with " +
                    bbox.length + " elements.");
        }

        return buildEnvelope(new double[]{
                bbox[0].doubleValue(),
                bbox[1].doubleValue(),
                bbox[2].doubleValue(),
                bbox[3].doubleValue()
        });
    }

    public static double[] getbbox(Envelope envelope) {
        return new double[]{
                envelope.getMinX(),
                envelope.getMinY(),
                envelope.getMaxX(),
                envelope.getMaxY()
        };
    }
}
