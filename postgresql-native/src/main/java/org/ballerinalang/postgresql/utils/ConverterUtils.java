/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.postgresql.utils;

import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.postgresql.Constants;
import org.ballerinalang.sql.exception.ApplicationError;
import org.postgresql.geometric.PGbox;
import org.postgresql.geometric.PGcircle;
import org.postgresql.geometric.PGline;
import org.postgresql.geometric.PGlseg;
import org.postgresql.geometric.PGpath;
import org.postgresql.geometric.PGpoint;
import org.postgresql.geometric.PGpolygon;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGInterval;
import org.postgresql.util.PGmoney;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.ballerina.runtime.api.utils.StringUtils.fromString;

/**
 * This class implements the utils methods for the PostgreSQL Datatypes.
 */

public class ConverterUtils {

    private static final ArrayType mapArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_MAP);

    private ConverterUtils() {
    }

    public static PGobject convertInet(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject inet = setPGobject(Constants.PGtypes.INET, stringValue);
        return inet;
    }

    public static PGobject convertCidr(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject cidr = setPGobject(Constants.PGtypes.CIDR, stringValue);
        return cidr;
    }

    public static PGobject convertMac(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject macaddr = setPGobject(Constants.PGtypes.MACADDR, stringValue);
        return macaddr;
    }

    public static PGobject convertMac8(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject macaddr8 = setPGobject(Constants.PGtypes.MACADDR8, stringValue);
        return macaddr8;
    }

    public static PGpoint convertPoint(Object value) throws SQLException {
        PGpoint point; 
        if (value instanceof BString) {
            try {
                point = new PGpoint(value.toString());
            } catch (SQLException ex) {
                throw new SQLException("Unsupported Value: " + value + " for type: " + "point");
            }
        } else {
            Map<String, Object> pointValue = ConversionHelperUtils.getRecordType(value);

            point = new PGpoint(
                ((BDecimal) (pointValue.get(Constants.Geometric.X))).decimalValue().doubleValue(),
                ((BDecimal) (pointValue.get(Constants.Geometric.Y))).decimalValue().doubleValue()
            );                
        }
        return point;
    }

    public static PGline convertLine(Object value) throws SQLException, ApplicationError {
        PGline line;
        Type type = TypeUtils.getType(value);
        if (value instanceof BString) {
            try {
                line = new PGline(value.toString());
            } catch (SQLException ex) {
                throw new SQLException("Unsupported Value: " + value + " for type: " + "line");
            }
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, Object> lineValue = ConversionHelperUtils.getRecordType(value);

            if (lineValue.containsKey(Constants.Geometric.A) && lineValue
                        .containsKey(Constants.Geometric.B)
                        && lineValue.containsKey(Constants.Geometric.C)) {
                    line = new PGline(
                        ((BDecimal) (lineValue.get(Constants.Geometric.A))).decimalValue().doubleValue(),
                        ((BDecimal) (lineValue.get(Constants.Geometric.B))).decimalValue().doubleValue(),
                        ((BDecimal) (lineValue.get(Constants.Geometric.C))).decimalValue().doubleValue()
                    );    
            } else {
                throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Line Datatype");
            }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Line Datatype");
        }
        return line;
    }

    public static PGlseg convertLseg(Object value) throws SQLException, ApplicationError {
        PGlseg lseg;
        Type type = TypeUtils.getType(value);
        if (value instanceof BString) {
            try {
                lseg = new PGlseg(value.toString());
            } catch (SQLException ex) {
                throw new SQLException("Unsupported Value: " + value + " for type: " + "lseg");
            }
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, Object> lsegValue = ConversionHelperUtils.getRecordType(value);

            if (lsegValue.containsKey(Constants.Geometric.X1) && lsegValue
                    .containsKey(Constants.Geometric.Y1)
            && lsegValue.containsKey(Constants.Geometric.X2) && lsegValue
                    .containsKey(Constants.Geometric.Y2)) {
                lseg = new PGlseg(
                    ((BDecimal) (lsegValue.get(Constants.Geometric.X1))).decimalValue().doubleValue(),
                    ((BDecimal) (lsegValue.get(Constants.Geometric.Y1))).decimalValue().doubleValue(),
                    ((BDecimal) (lsegValue.get(Constants.Geometric.X2))).decimalValue().doubleValue(),
                    ((BDecimal) (lsegValue.get(Constants.Geometric.Y2))).decimalValue().doubleValue()
                );  
            } else {
                throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Lseg Datatype");
            }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Lseg Datatype");
        }
        return lseg;
    }


    public static PGbox convertBox(Object value) throws SQLException, ApplicationError {
        PGbox box;
        Type type = TypeUtils.getType(value);
        if (value instanceof BString) {
            try {
                box = new PGbox(value.toString());
            } catch (SQLException ex) {
                throw new SQLException("Unsupported Value: " + value + " for type: " + "box");
            }
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, Object> boxValue = ConversionHelperUtils.getRecordType(value);

            if (boxValue.containsKey(Constants.Geometric.X1) && boxValue
                    .containsKey(Constants.Geometric.Y1)
            && boxValue.containsKey(Constants.Geometric.X2) && boxValue
                    .containsKey(Constants.Geometric.Y2)) {
                box = new PGbox(
                    ((BDecimal) (boxValue.get(Constants.Geometric.X1))).decimalValue().doubleValue(),
                    ((BDecimal) (boxValue.get(Constants.Geometric.Y1))).decimalValue().doubleValue(),
                    ((BDecimal) (boxValue.get(Constants.Geometric.X2))).decimalValue().doubleValue(),
                    ((BDecimal) (boxValue.get(Constants.Geometric.Y2))).decimalValue().doubleValue()
                );  
            } else {
                throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Box Datatype");
            }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Box Datatype");
        }
        return box;
    }

    public static PGpath convertPath(Object value) throws SQLException, ApplicationError {
        PGpath path;
        Type type = TypeUtils.getType(value);
        if (value instanceof BString) {
            try {
                path = new PGpath(value.toString());
            } catch (SQLException ex) {
                throw new SQLException("Unsupported Value: " + value + " for type: " + "path");
            }
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            PGpoint pgpoint;
            ArrayList<Object> pointsArray = ConversionHelperUtils.getArrayType((BArray) value);
            if (pointsArray.size() == 0) {
                throw new SQLException("No points were found for Path type");
            }
            PGpoint[] points = new PGpoint[pointsArray.size()];
            for (int i = 0; i < pointsArray.size(); i++) {
                pgpoint = convertPoint(pointsArray.get(i));
                points[i] = pgpoint;
            }
            path = new PGpath(points, false);
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            PGpoint pgpoint;
            Map<String, Object> pathValue = ConversionHelperUtils.getRecordType(value);
            if (pathValue.containsKey(Constants.Geometric.POINTS) && 
                    pathValue.containsKey(Constants.Geometric.OPEN)) {
                boolean open = ((Boolean) (pathValue.get(Constants.Geometric.OPEN))).booleanValue();
                ArrayList<Object> pointsArray = ConversionHelperUtils.getArrayType((BArray) pathValue
                                    .get(Constants.Geometric.POINTS));
                if (pointsArray.size() == 0) {
                    throw new SQLException("No points were found for Path type");
                }
                PGpoint[] points = new PGpoint[pointsArray.size()];
                for (int i = 0; i < pointsArray.size(); i++) {
                    pgpoint = convertPoint(pointsArray.get(i));
                    points[i] = pgpoint;
                }
                path = new PGpath(points, open);
            } else {
                throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Path Datatype");
            }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Path Datatype");
        }
        return path;
    }

    public static PGpolygon convertPolygon(Object value) throws SQLException, ApplicationError {
        PGpolygon polygon;
        Type type = TypeUtils.getType(value);
        if (value instanceof BString) {
            try {
                polygon = new PGpolygon(value.toString());
            } catch (SQLException ex) {
                throw new SQLException("Unsupported Value: " + value + " for type: " + "polygon");
            }
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            PGpoint pgpoint;
            ArrayList<Object> pointsArray = ConversionHelperUtils.getArrayType((BArray) value);
            if (pointsArray.size() == 0) {
                throw new SQLException("No points were found for Polygon type");
            }
            PGpoint[] points = new PGpoint[pointsArray.size()];
            for (int i = 0; i < pointsArray.size(); i++) {
                pgpoint = convertPoint(pointsArray.get(i));
                points[i] = pgpoint;
            }
            polygon = new PGpolygon(points);
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Polygon Datatype");
        }
        return polygon;
    }

    public static PGcircle convertCircle(Object value) throws SQLException, ApplicationError {
        PGcircle circle;
        Type type = TypeUtils.getType(value);
        if (value instanceof BString) {
            try {
                circle = new PGcircle(value.toString());
            } catch (SQLException ex) {
                throw new SQLException("Unsupported Value: " + value + " for type: " + "circle");
            }
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, Object> circleValue = ConversionHelperUtils.getRecordType(value);
            if (circleValue.containsKey(Constants.Geometric.X) && circleValue
                    .containsKey(Constants.Geometric.Y)
                && circleValue.containsKey(Constants.Geometric.R)) {
                circle = new PGcircle(
                    ((BDecimal)  (circleValue.get(Constants.Geometric.X))).decimalValue().doubleValue(),
                    ((BDecimal)  (circleValue.get(Constants.Geometric.Y))).decimalValue().doubleValue(),
                    ((BDecimal)  (circleValue.get(Constants.Geometric.R))).decimalValue().doubleValue()
                );  
            } else {
                throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Circle Datatype");
            }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Circle Datatype");
        }
        return circle;
    }

    public static PGobject convertUuid(Object value) throws SQLException {
        String stringValue = value.toString();

        PGobject uuid = setPGobject(Constants.PGtypes.UUID, stringValue);
        return uuid;
    }

    public static PGobject convertTsVector(Object value) throws SQLException {
        String stringValue = value.toString();

        PGobject tsvector = setPGobject(Constants.PGtypes.TSVECTOR, stringValue);
        return tsvector;
    }

    public static PGobject convertTsQuery(Object value) throws SQLException {
        String stringValue = value.toString();

        PGobject tsquery = setPGobject(Constants.PGtypes.TSQUERY, stringValue);
        return tsquery;
    }

    public static PGobject convertJson(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject json = setPGobject(Constants.PGtypes.JSON, stringValue);
        return json;
    }

    public static PGobject convertJsonb(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject jsonb = setPGobject(Constants.PGtypes.JSONB, stringValue);
        return jsonb;
    }

    public static PGobject convertJsonPath(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject jsonpath = setPGobject(Constants.PGtypes.JSONPATH, stringValue);
        return jsonpath;
    }

    public static PGInterval convertInterval(Object value) throws SQLException, ApplicationError {
        Type type = TypeUtils.getType(value);
        PGInterval interval; 
        if (value instanceof BString) {
            try {
                interval = new PGInterval(value.toString());
            } catch (SQLException ex) {
                throw new SQLException("Unsupported Value: " + value + " for type: " + "interval");
            }
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, Object> intervalValue = ConversionHelperUtils.getRecordType(value);

            if (intervalValue.containsKey(Constants.Interval.YEARS) && intervalValue
                    .containsKey(Constants.Interval.MONTHS)
                && intervalValue.containsKey(Constants.Interval.DAYS) && intervalValue
                        .containsKey(Constants.Interval.HOURS)
                && intervalValue.containsKey(Constants.Interval.MINUTES) && intervalValue
                        .containsKey(Constants.Interval.SECONDS)) {

                interval = new PGInterval(
                    ((Number) (intervalValue.get(Constants.Interval.YEARS))).intValue(),
                    ((Number) (intervalValue.get(Constants.Interval.MONTHS))).intValue(),
                    ((Number) (intervalValue.get(Constants.Interval.DAYS))).intValue(),
                    ((Number) (intervalValue.get(Constants.Interval.HOURS))).intValue(),
                    ((Number) (intervalValue.get(Constants.Interval.MINUTES))).intValue(),
                    ((BDecimal) (intervalValue.get(Constants.Interval.SECONDS))).decimalValue().doubleValue()
                );
            } else {
                throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Interval Datatype");
            }

        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Interval Datatype");
        }
        return interval;
    }

    public static PGobject convertInt4Range(Object value) throws SQLException, ApplicationError {
        Type type = TypeUtils.getType(value);
        PGobject int4rangeObject; 
        if (value instanceof BString) {
            String stringValue = value.toString();
            int4rangeObject = setPGobject(Constants.PGtypes.INT4RANGE, stringValue);
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, Object> rangeValue = ConversionHelperUtils.getRecordType(value);

            if (rangeValue.containsKey(Constants.Range.UPPER) && rangeValue
                    .containsKey(Constants.Range.LOWER)
                && rangeValue.containsKey(Constants.Range.UPPERINCLUSIVE) && rangeValue
                    .containsKey(Constants.Range.LOWERINCLUSIVE)) {

                String upperValue = rangeValue.get(Constants.Range.UPPER).toString();
                String lowerValue = rangeValue.get(Constants.Range.LOWER).toString();
                boolean upperInclusive = ((Boolean) (rangeValue
                    .get(Constants.Range.UPPERINCLUSIVE))).booleanValue();
                boolean lowerInclusive = ((Boolean) (rangeValue
                    .get(Constants.Range.LOWERINCLUSIVE))).booleanValue();

                String range = ConversionHelperUtils
                    .setRange(upperValue, lowerValue, upperInclusive, lowerInclusive);

                int4rangeObject = setPGobject(Constants.PGtypes.INT4RANGE, range);
            } else {
                throw new SQLException("Unsupported Ballerina type for PostgreSQL Int4Range type");
            }

        } else {
            throw new SQLException("Unsupported Ballerina type for PostgreSQL Int4Range type");
        }
        return int4rangeObject;
    }

    public static PGobject convertInt8Range(Object value) throws SQLException, ApplicationError {
        Type type = TypeUtils.getType(value);
        PGobject int8rangeObject; 
        if (value instanceof BString) {
            String stringValue = value.toString();
            int8rangeObject = setPGobject(Constants.PGtypes.INT8RANGE, stringValue);
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, Object> rangeValue = ConversionHelperUtils.getRecordType(value);
            if (rangeValue.containsKey(Constants.Range.UPPER) && rangeValue
                    .containsKey(Constants.Range.LOWER)
                && rangeValue.containsKey(Constants.Range.UPPERINCLUSIVE) && rangeValue
                        .containsKey(Constants.Range.LOWERINCLUSIVE)) {
                String upperValue = rangeValue.get(Constants.Range.UPPER).toString();
                String lowerValue = rangeValue.get(Constants.Range.LOWER).toString();
                boolean upperInclusive = ((Boolean) (rangeValue
                        .get(Constants.Range.UPPERINCLUSIVE))).booleanValue();
                boolean lowerInclusive = ((Boolean) (rangeValue
                        .get(Constants.Range.LOWERINCLUSIVE))).booleanValue();

                String range = ConversionHelperUtils
                        .setRange(upperValue, lowerValue, upperInclusive, lowerInclusive);

                int8rangeObject = setPGobject(Constants.PGtypes.INT8RANGE, range);
            } else {
                throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Int8range Datatype");
            }

        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Int8range Datatype");
        }
        return int8rangeObject;
    }

    public static PGobject convertNumRange(Object value) throws SQLException, ApplicationError {
        Type type = TypeUtils.getType(value);
        PGobject numrangeObject; 
        if (value instanceof BString) {
            String stringValue = value.toString();
            numrangeObject = setPGobject(Constants.PGtypes.NUMRANGE, stringValue);
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, Object> rangeValue = ConversionHelperUtils.getRecordType(value);
            if (rangeValue.containsKey(Constants.Range.UPPER) && rangeValue
                        .containsKey(Constants.Range.LOWER)
                && rangeValue.containsKey(Constants.Range.UPPERINCLUSIVE) && rangeValue
                        .containsKey(Constants.Range.LOWERINCLUSIVE)) {
                String upperValue = rangeValue.get(Constants.Range.UPPER).toString();
                String lowerValue = rangeValue.get(Constants.Range.LOWER).toString();
                boolean upperInclusive = ((Boolean) (rangeValue
                        .get(Constants.Range.UPPERINCLUSIVE))).booleanValue();
                boolean lowerInclusive = ((Boolean) (rangeValue
                        .get(Constants.Range.LOWERINCLUSIVE))).booleanValue();
                String range = ConversionHelperUtils
                        .setRange(upperValue, lowerValue, upperInclusive, lowerInclusive);
                numrangeObject = setPGobject(Constants.PGtypes.NUMRANGE, range);
            } else {
                throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Numeric range Datatype");
            }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Numeric range Datatype");
        }
        return numrangeObject;
    }

    public static PGobject convertTsRange(Object value) throws SQLException, ApplicationError {
        String upperValue, lowerValue;
        Type type = TypeUtils.getType(value);
        PGobject tsrangeObject; 
        if (value instanceof BString) {
            String stringValue = value.toString();
            tsrangeObject = setPGobject(Constants.PGtypes.TSRANGE, stringValue);
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            try {
                Map<String, Object> rangeValue = ConversionHelperUtils.getRecordType(value);
                if (rangeValue.containsKey(Constants.Range.UPPER) && rangeValue.containsKey(Constants.Range.LOWER)
                    && rangeValue.containsKey(Constants.Range.UPPERINCLUSIVE) && rangeValue
                            .containsKey(Constants.Range.LOWERINCLUSIVE)) {
                    Object upperObj = rangeValue.get(Constants.Range.UPPER);
                    type = TypeUtils.getType(upperObj);
                    if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
                        upperValue = ConversionHelperUtils.convertCivilToString(upperObj, false);
                    } else {
                        upperValue = upperObj.toString();
                    }
                    Object lowerObj = rangeValue.get(Constants.Range.LOWER);
                    type = TypeUtils.getType(lowerObj);
                    if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
                        lowerValue = ConversionHelperUtils.convertCivilToString(lowerObj, false);
                    } else {
                        lowerValue = lowerObj.toString();
                    }
                    boolean upperInclusive = ((Boolean) (rangeValue
                            .get(Constants.Range.UPPERINCLUSIVE))).booleanValue();
                    boolean lowerInclusive = ((Boolean) (rangeValue
                            .get(Constants.Range.LOWERINCLUSIVE))).booleanValue();
                    String range = ConversionHelperUtils
                            .setRange(upperValue, lowerValue, upperInclusive, lowerInclusive);

                    tsrangeObject = setPGobject(Constants.PGtypes.TSRANGE, range);
                } else {
                    throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Timestamp range Datatype");
                }
           } catch (DateTimeException ex) {
            throw new SQLException(ex.getMessage());
           }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Timestamp range Datatype");
        }
        return tsrangeObject;
    }

    public static PGobject convertTstzRange(Object value) throws SQLException, ApplicationError {
        String upperValue, lowerValue;
        Type type = TypeUtils.getType(value);
        PGobject tstzrangeObject; 
        if (value instanceof BString) {
            String stringValue = value.toString();
            tstzrangeObject = setPGobject(Constants.PGtypes.TSTZRANGE, stringValue);
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            try {
                Map<String, Object> rangeValue = ConversionHelperUtils.getRecordType(value);
                if (rangeValue.containsKey(Constants.Range.UPPER) && rangeValue.containsKey(Constants.Range.LOWER)
                    && rangeValue.containsKey(Constants.Range.UPPERINCLUSIVE) && rangeValue
                            .containsKey(Constants.Range.LOWERINCLUSIVE)) {
                    Object upperObj = rangeValue.get(Constants.Range.UPPER);
                    type = TypeUtils.getType(upperObj);
                    if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
                        upperValue = ConversionHelperUtils.convertCivilToString(upperObj, true);
                    } else {
                        upperValue = upperObj.toString();
                    }
                    Object lowerObj = rangeValue.get(Constants.Range.LOWER);
                    type = TypeUtils.getType(lowerObj);
                    if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
                        lowerValue = ConversionHelperUtils.convertCivilToString(lowerObj, true);
                    } else {
                        lowerValue = lowerObj.toString();
                    }
                    boolean upperInclusive = ((Boolean) (rangeValue
                            .get(Constants.Range.UPPERINCLUSIVE))).booleanValue();
                    boolean lowerInclusive = ((Boolean) (rangeValue
                            .get(Constants.Range.LOWERINCLUSIVE))).booleanValue();
                    String range = ConversionHelperUtils
                            .setRange(upperValue, lowerValue, upperInclusive, lowerInclusive);
                    tstzrangeObject = setPGobject(Constants.PGtypes.TSTZRANGE, range);
                } else {
                    throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL TimestampTz range Datatype");
                }
            } catch (DateTimeException ex) {
                    throw new SQLException(ex.getMessage());
            }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL TimestampTz range Datatype");
        }
            return tstzrangeObject;
    }

    public static PGobject convertDateRange(Object value) throws SQLException, ApplicationError {
        String upperValue, lowerValue;
        Type type = TypeUtils.getType(value);
        PGobject daterangeObject; 
        if (value instanceof BString) {
            String stringValue = value.toString();
            daterangeObject = setPGobject(Constants.PGtypes.DATERANGE, stringValue);
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            try {
                Map<String, Object> rangeValue = ConversionHelperUtils.getRecordType(value);
                if (rangeValue.containsKey(Constants.Range.UPPER) && rangeValue.containsKey(Constants.Range.LOWER)
                    && rangeValue.containsKey(Constants.Range.UPPERINCLUSIVE) && rangeValue
                            .containsKey(Constants.Range.LOWERINCLUSIVE)) {
                    Object upperObj = rangeValue.get(Constants.Range.UPPER);
                    type = TypeUtils.getType(upperObj);
                    if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
                        upperValue = ConversionHelperUtils.convertDateToString(upperObj);
                    } else {
                        upperValue = upperObj.toString();
                    }
                    Object lowerObj = rangeValue.get(Constants.Range.LOWER);
                    type = TypeUtils.getType(lowerObj);
                    if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
                        lowerValue = ConversionHelperUtils.convertDateToString(lowerObj);
                    } else {
                        lowerValue = lowerObj.toString();
                    }
                    boolean upperInclusive = ((Boolean) (rangeValue
                            .get(Constants.Range.UPPERINCLUSIVE))).booleanValue();
                    boolean lowerInclusive = ((Boolean) (rangeValue
                            .get(Constants.Range.LOWERINCLUSIVE))).booleanValue();
                    String range = ConversionHelperUtils
                            .setRange(upperValue, lowerValue, upperInclusive, lowerInclusive);
                    daterangeObject = setPGobject(Constants.PGtypes.DATERANGE, range);
                } else {
                    throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Date range Datatype");
                }
            } catch (DateTimeException ex) {
                    throw new SQLException(ex.getMessage());
            }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Date Range Datatype");
        }
        return daterangeObject;
    }

    public static PGobject convertPglsn(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject pglsn = setPGobject(Constants.PGtypes.PGLSN, stringValue);
        return pglsn;
    }

    public static PGobject convertBitn(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject bitn = setPGobject(Constants.PGtypes.BITSTRING, stringValue);
        return bitn;
    }

    public static PGobject convertVarbit(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject varbit = setPGobject(Constants.PGtypes.VARBITSTRING, stringValue);
        return varbit;
    }

    public static PGobject convertBit(Object value) throws SQLException {
        String stringValue;
        if (value instanceof Boolean) {
            Boolean booleanValue = (Boolean) value;
            stringValue = booleanValue ? "1" : "0";
        } else {
            stringValue = value.toString();
        }
        PGobject bit = setPGobject(Constants.PGtypes.PGBIT, stringValue);
        return bit;
    }

    public static PGmoney convertMoney(Object value) throws SQLException {
        PGmoney money;
        if (value instanceof BString) {
            String stringValue = value.toString();
            money = setPGmoney(stringValue);
        } else if (value instanceof BDecimal) {
            double doubleValue = ((BDecimal) value).decimalValue().doubleValue();
            money = setPGmoney(doubleValue);
        } else if (value instanceof Double) {
            double doubleValue = ((Double) value).doubleValue();
            money = setPGmoney(doubleValue);
        } else {
            throw new SQLException("Unsupported Value: " + value + " for type: " + "money");
        }
        return money;
    }

    public static PGobject convertCustomType(Object value) throws SQLException, ApplicationError {
        String stringValue;
        Type type = TypeUtils.getType(value);
        if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, Object> customRecord = ConversionHelperUtils.getRecordType(value);
            if (customRecord.containsKey(Constants.Custom.TYPE) && 
                    customRecord.containsKey(Constants.Custom.VALUES)) {
                String typeName = customRecord.get(Constants.Custom.TYPE).toString();  
                customRecord = ConversionHelperUtils.
                        getRecordType(customRecord.get(Constants.Custom.VALUES));
                ArrayList<Object> objectArray = ConversionHelperUtils.getArrayType
                        ((BArray) customRecord.get(Constants.Custom.VALUES));
                stringValue = ConversionHelperUtils.convertCustomType(objectArray);
                return setPGobject(typeName, stringValue);
            } else {
                throw new ApplicationError("Unsupported Record Type for PostgreSQL Custom Datatype");
            }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Custom Datatype");
        }
    }

    public static PGobject convertEnum(Object value) throws SQLException, ApplicationError {
        Type type = TypeUtils.getType(value);
        if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, Object> customRecord = ConversionHelperUtils.getRecordType(value);
            if (customRecord.containsKey(Constants.Custom.TYPE) && 
                    customRecord.containsKey(Constants.Custom.VALUE)) {
                String typeName = customRecord.get(Constants.Custom.TYPE).toString();
                Object enumRecord = customRecord.get(Constants.Custom.VALUE);
                if (enumRecord == null) {
                    return null;
                }
                customRecord = ConversionHelperUtils.
                        getRecordType(customRecord.get(Constants.Custom.VALUE));
                String valueName = customRecord.get(Constants.Custom.VALUE).toString();
                return setPGobject(typeName, valueName);
            } else {
                throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Enum Datatype");
            }
        } else {
            throw new ApplicationError("Unsupported Ballerina Type for PostgreSQL Enum Datatype");
        }
    }


    public static PGobject convertRegclass(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject regclass = setPGobject(Constants.PGtypes.REGCLASS, stringValue);
        return regclass;
    }

    public static PGobject convertRegconfig(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject regconfig = setPGobject(Constants.PGtypes.REGCONFIG, stringValue);
        return regconfig;
    }

    public static PGobject convertRegdictionary(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject regdictionary = setPGobject(Constants.PGtypes.REGDICTIONARY, stringValue);
        return regdictionary;
    }

    public static PGobject convertRegnamespace(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject regnamespace = setPGobject(Constants.PGtypes.REGNAMESPACE, stringValue);
        return regnamespace;
    }

    public static PGobject convertRegoper(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject regoper = setPGobject(Constants.PGtypes.REGOPER, stringValue);
        return regoper;
    }

    public static PGobject convertRegoperator(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject regoperator = setPGobject(Constants.PGtypes.REGOPERATOR, stringValue);
        return regoperator;
    }

    public static PGobject convertRegproc(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject regproc = setPGobject(Constants.PGtypes.REGPROC, stringValue);
        return regproc;
    }

    public static PGobject convertRegprocedure(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject regprocedure = setPGobject(Constants.PGtypes.REGPROCEDURE, stringValue);
        return regprocedure;
    }

    public static PGobject convertRegrole(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject regrole = setPGobject(Constants.PGtypes.REGROLE, stringValue);
        return regrole;
    }

    public static PGobject convertRegtype(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject regtype = setPGobject(Constants.PGtypes.REGTYPE, stringValue);
        return regtype;
    }

    public static Object convertXml(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject xml = setPGobject(Constants.PGtypes.XML, stringValue);
        return xml;
    }

    public static PGobject convertTimetz(Object value) throws SQLException {
        String stringValue = value.toString();
        PGobject timetz = setPGobject(Constants.PGtypes.TIMETZ, stringValue);
        return timetz;
    }

    public static BMap convertIntervalToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap = new HashMap<>();
        if (value == null) {
            return null;
        }
        try {
            PGInterval interval = new PGInterval(value.toString());
            valueMap.put(Constants.Interval.YEARS, interval.getYears());
            valueMap.put(Constants.Interval.MONTHS, interval.getMonths());
            valueMap.put(Constants.Interval.DAYS, interval.getDays());
            valueMap.put(Constants.Interval.HOURS, interval.getHours());
            valueMap.put(Constants.Interval.MINUTES, interval.getMinutes());
            valueMap.put(Constants.Interval.SECONDS, ValueCreator.createDecimalValue(
                    new BigDecimal(interval.getSeconds())));

            return ValueCreator.createRecordValue(ModuleUtils.getModule(),
                typeName, valueMap);
        } catch (SQLException  ex) {
            throw new SQLException("Unsupported Type " + typeName + "You have to use postgresql:" +
                    Constants.TypeRecordNames.INTERVALRECORD);
        }
    }

    public static BMap convertPointToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap = new HashMap<>();
        if (value == null) {
            return null;
        }
        try {
            PGpoint point = new PGpoint(value.toString());
            valueMap.put(Constants.Geometric.X, ValueCreator.createDecimalValue(
                    new BigDecimal(point.x)));
            valueMap.put(Constants.Geometric.Y, ValueCreator.createDecimalValue(
                    new BigDecimal(point.y)));
            return ValueCreator.createRecordValue(ModuleUtils.getModule(),
            typeName, valueMap);
        } catch (SQLException  ex) {
            throw new SQLException("Unsupported Type " + typeName + "You have to use postgresql:" +
                    Constants.TypeRecordNames.POINTRECORD);
        }
    }

    public static BMap convertLineToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap = new HashMap<>();
        if (value == null) {
            return null;
        }
        try {
            PGline line = new PGline(value.toString());
            valueMap.put(Constants.Geometric.A, ValueCreator.createDecimalValue(
                    new BigDecimal(line.a)));
            valueMap.put(Constants.Geometric.B, ValueCreator.createDecimalValue(
                    new BigDecimal(line.b)));
            valueMap.put(Constants.Geometric.C, ValueCreator.createDecimalValue(
                    new BigDecimal(line.c)));

        return ValueCreator.createRecordValue(ModuleUtils.getModule(),
            typeName, valueMap);
        } catch (SQLException  ex) {
            throw new SQLException("Unsupported Type " + typeName + "You have to use postgresql:" +
                    Constants.TypeRecordNames.LINERECORD);
        }
    }

    public static BMap convertLsegToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap = new HashMap<>();
        if (value == null) {
            return null;
        }
        try {
            PGlseg lseg = new PGlseg(value.toString());
            PGpoint[] points = lseg.point;
            PGpoint point1 = points[0];
            PGpoint point2 = points[1];
            valueMap.put(Constants.Geometric.X1, ValueCreator.createDecimalValue(new BigDecimal(point1.x)));
            valueMap.put(Constants.Geometric.Y1, ValueCreator.createDecimalValue(new BigDecimal(point1.y)));
            valueMap.put(Constants.Geometric.X2, ValueCreator.createDecimalValue(new BigDecimal(point2.x)));
            valueMap.put(Constants.Geometric.Y2, ValueCreator.createDecimalValue(new BigDecimal(point2.y)));

            return ValueCreator.createRecordValue(ModuleUtils.getModule(),
                typeName, valueMap);
            } catch (SQLException  ex) {
                throw new SQLException("Unsupported Type " + typeName + "You have to use postgresql:" +
                        Constants.TypeRecordNames.LSEGRECORD);
            }
    }

    public static BMap convertBoxToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap = new HashMap<>();
        if (value == null) {
            return null;
        }
        try {
            PGbox box = new PGbox(value.toString());
            PGpoint[] points = box.point;
            PGpoint point1 = points[1];
            PGpoint point2 = points[0];
            valueMap.put(Constants.Geometric.X1, ValueCreator.createDecimalValue(new BigDecimal(point1.x)));
            valueMap.put(Constants.Geometric.Y1, ValueCreator.createDecimalValue(new BigDecimal(point1.y)));
            valueMap.put(Constants.Geometric.X2, ValueCreator.createDecimalValue(new BigDecimal(point2.x)));
            valueMap.put(Constants.Geometric.Y2, ValueCreator.createDecimalValue(new BigDecimal(point2.y)));

            return ValueCreator.createRecordValue(ModuleUtils.getModule(),
                typeName, valueMap);
            } catch (SQLException  ex) {
                throw new SQLException("Unsupported Type " + typeName + "You have to use postgresql:" +
                        Constants.TypeRecordNames.BOXRECORD);
            }
    }

    public static BMap convertPathToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap = new HashMap<>();
        PGpoint point;
        if (value == null) {
            return null;
        }
        try {
            PGpath path = new PGpath(value.toString());
            PGpoint[] points = path.points;
            BArray mapDataArray = ValueCreator.createArrayValue(Constants.POINT_ARRAY_TYPE);
            for (var i = 0; i < points.length; i++) {
                point = points[i];
                mapDataArray.add(i, convertPointToRecord(point, Constants.TypeRecordNames.POINTRECORD));
            }
            valueMap.put(Constants.Geometric.OPEN, path.open);
            valueMap.put(Constants.Geometric.POINTS, mapDataArray);
    
            return ValueCreator.createRecordValue(ModuleUtils.getModule(),
                typeName, valueMap);
            } catch (SQLException  ex) {
                throw new SQLException("Unsupported Type " + typeName + "You have to use postgresql:" +
                        Constants.TypeRecordNames.PATHRECORD);
            }
    }

    public static BMap convertPolygonToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap = new HashMap<>();
        PGpoint point;
        if (value == null) {
            return null;
        }
        try {
            PGpolygon polygon = new PGpolygon(value.toString());
            PGpoint[] points = polygon.points;
            BArray mapDataArray = ValueCreator.createArrayValue(Constants.POINT_ARRAY_TYPE);
            for (var i = 0; i < points.length; i++) {
                point = points[i];
                mapDataArray.add(i, convertPointToRecord(point, Constants.TypeRecordNames.POINTRECORD));
            }
            valueMap.put(Constants.Geometric.POINTS, mapDataArray);
    
            return ValueCreator.createRecordValue(ModuleUtils.getModule(),
                typeName, valueMap);
            } catch (SQLException  ex) {
                throw new SQLException("Unsupported Type " + typeName + "You have to use postgresql:" +
                        Constants.TypeRecordNames.POLYGONRECORD);
            }
    }

    public static BMap convertCircleToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap = new HashMap<>();
        if (value == null) {
            return null;
        }
        try {
            PGcircle circle = new PGcircle(value.toString());
            PGpoint center = circle.center;
            valueMap.put(Constants.Geometric.X, ValueCreator.createDecimalValue(new BigDecimal(center.x)));
            valueMap.put(Constants.Geometric.Y, ValueCreator.createDecimalValue(new BigDecimal(center.y)));
            valueMap.put(Constants.Geometric.R, ValueCreator.createDecimalValue(new BigDecimal(circle.radius)));

            return ValueCreator.createRecordValue(ModuleUtils.getModule(),
                typeName, valueMap);
            } catch (SQLException  ex) {
                throw new SQLException("Unsupported Type " + typeName + "You have to use postgresql:" +
                        Constants.TypeRecordNames.LSEGRECORD);
            }
    }

    public static BMap convertInt4rangeToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap;
        if (value == null) {
            return null;
        }
        valueMap = ConversionHelperUtils.convertRangeToMap(value);

        int upperValue = Integer.parseInt(valueMap.get(Constants.Range.UPPER).toString());
        valueMap.put(Constants.Range.UPPER, upperValue);

        int lowerValue = Integer.parseInt(valueMap.get(Constants.Range.LOWER).toString());
        valueMap.put(Constants.Range.LOWER, lowerValue);

        return ValueCreator.createRecordValue(ModuleUtils.getModule(),
            typeName, valueMap);
    }

    public static BMap convertInt8rangeToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap;
        if (value == null) {
            return null;
        }
        valueMap = ConversionHelperUtils.convertRangeToMap(value);

        long upperValue = Long.parseLong(valueMap.get(Constants.Range.UPPER).toString());
        valueMap.put(Constants.Range.UPPER, upperValue);

        long lowerValue = Long.parseLong(valueMap.get(Constants.Range.LOWER).toString());
        valueMap.put(Constants.Range.LOWER, lowerValue);

        return ValueCreator.createRecordValue(ModuleUtils.getModule(),
            typeName, valueMap);
    }

    public static BMap convertNumrangeToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap;
        if (value == null) {
            return null;
        }
        valueMap = ConversionHelperUtils.convertRangeToMap(value);
        valueMap.put(Constants.Range.UPPER, ValueCreator.createDecimalValue(new BigDecimal(valueMap.get(Constants.
                Range.UPPER).toString())));
        valueMap.put(Constants.Range.LOWER, ValueCreator.createDecimalValue(new BigDecimal(valueMap.get(Constants.
                Range.LOWER).toString())));

        return ValueCreator.createRecordValue(ModuleUtils.getModule(),
            typeName, valueMap);
    }

    public static BMap converTsrangeToRecord(Object value, String typeName) throws SQLException {
        if (typeName.equals(Constants.TypeRecordNames.TIMESTAMP_RANGE_RECORD_CIVIL)) {
            return convertTimestampRangeToCivil(value, typeName);
        } else {
            return convertTimestampRangeToRecord(value, typeName);
        }
    }

    public static BMap convertTstzrangeToRecord(Object value, String typeName) throws SQLException {
        if (typeName.equals(Constants.TypeRecordNames.TIMESTAMPTZ_RANGE_RECORD_CIVIL)) {
            return convertTimestampRangeToCivil(value, typeName);
        } else {
            return convertTimestampRangeToRecord(value, typeName);
        }
    }

    public static BMap convertDaterangeToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap;
        if (value == null) {
            return null;
        } else if (typeName.equals(Constants.TypeRecordNames.DATERANGE_RECORD_TYPE)) {
            try {
                valueMap = ConversionHelperUtils.convertRangeToMap(value);
                String upperValue = valueMap.get(Constants.Range.UPPER).toString();
                valueMap.put(Constants.Range.UPPER, ConversionHelperUtils.convertISOStringToDate(upperValue));
                String lowerValue = valueMap.get(Constants.Range.LOWER).toString();
                valueMap.put(Constants.Range.LOWER, ConversionHelperUtils.convertISOStringToDate(lowerValue));
                return ValueCreator.createRecordValue(ModuleUtils.getModule(),
                    typeName, valueMap);
            } catch (DateTimeException ex) {
                throw new SQLException(ex.getMessage());
            } 
        } else {
            valueMap = ConversionHelperUtils.convertRangeToMap(value);
            return ValueCreator.createRecordValue(ModuleUtils.getModule(),
                typeName, valueMap);
        }
    }

    private static BMap convertTimestampRangeToRecord(Object value, String typeName) throws SQLException {
        Map<String, Object> valueMap;
        if (value == null) {
            return null;
        }
        valueMap = ConversionHelperUtils.convertRangeToMap(value);
        String upperValue = valueMap.get(Constants.Range.UPPER).toString();
        valueMap.put(Constants.Range.UPPER, upperValue.substring(1, upperValue.length() - 1));
        String lowerValue = valueMap.get(Constants.Range.LOWER).toString();
        valueMap.put(Constants.Range.LOWER, lowerValue.substring(1, lowerValue.length() - 1));
        return ValueCreator.createRecordValue(ModuleUtils.getModule(),
            typeName, valueMap);
    }

    private static BMap<BString, Object> convertTimestampRangeToCivil(Object value, String typeName)
         throws SQLException {
        Map<String, Object> valueMap;
        if (value == null) {
            return null;
        }
        try {
            valueMap = ConversionHelperUtils.convertRangeToMap(value);
            String upperValue = valueMap.get(Constants.Range.UPPER).toString();
            valueMap.put(Constants.Range.UPPER, ConversionHelperUtils.convertISOStringToCivil(upperValue));
            String lowerValue = valueMap.get(Constants.Range.LOWER).toString();
            valueMap.put(Constants.Range.LOWER, ConversionHelperUtils.convertISOStringToCivil(lowerValue));
            return ValueCreator.createRecordValue(ModuleUtils.getModule(),
                typeName, valueMap);
        } catch (DateTimeException ex) {
            throw new SQLException(ex.getMessage());
        } 
    }

    public static BMap convertCustomTypeToRecord(Object value, String typeName) {
        Map<String, Object> valueMap = new HashMap<>();
        if (value == null) {
            return null;
        }
        valueMap.put(Constants.Custom.VALUES, ConversionHelperUtils.
                    convertCustomTypeToString(value.toString()));
        return ValueCreator.createRecordValue(ModuleUtils.getModule(),
                typeName, valueMap);
    }

    public static Object[] convertPointArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGpoint[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertPoint(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.POINT};
    }

    public static Object[] convertLineArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGline[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertLine(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.LINE};
    }

    public static Object[] convertLsegArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGlseg[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertLseg(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.LSEG};
    }

    public static Object[] convertBoxArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGbox[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertBox(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.BOX};
    }

    public static Object[] convertPathArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGpath[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertPath(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.PATH};
    }

    public static Object[] convertPolygonArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGpolygon[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertPolygon(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.POLYGON};
    }

    public static Object[] convertCircleArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGcircle[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertCircle(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.CIRCLE};
    }

    public static Object[] convertIntervalArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGInterval[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertInterval(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.INTERVAL};
    }

    public static Object[] convertInt4RangeArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertInt4Range(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.INT4RANGE};
    }

    public static Object[] convertInt8RangeArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertInt8Range(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.INT8RANGE};
    }

    public static Object[] convertNumRangeArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertNumRange(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.NUMRANGE};
    }

    public static Object[] convertTsRangeArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertTsRange(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.TSRANGE};
    }

    public static Object[] convertTstzRangeArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertTstzRange(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.TSTZRANGE};
    }

    public static Object[] convertDateRangeArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertDateRange(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.DATERANGE};
    }

    public static Object[] convertInetArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertInet(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.INET};
    }

    public static Object[] convertCidrArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertCidr(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.CIDR};
    }

    public static Object[] convertMacaddrArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertMac(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.MACADDR};
    }

    public static Object[] convertMacaddr8Array(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertMac8(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.MACADDR8};
    }

    public static Object[] convertUuidArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertUuid(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.UUID};
    }

    public static Object[] convertTsvectotArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertTsVector(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.TSVECTOR};
    }

    public static Object[] convertTsqueryArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertTsQuery(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.TSQUERY};
    }

    public static Object[] convertBitstringArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertVarbit(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.BITSTRING};
    }

    public static Object[] convertBitArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertBit(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.BIT_VARYING};
    }

    public static Object[] convertVarbitstringArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertVarbit(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.BIT};
    }

    public static Object[] convertXmlArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertXml(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.XML};
    }

    public static Object[] convertRegclassArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertRegclass(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.REGCLASS};
    }

    public static Object[] convertRegconfigArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertRegconfig(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.REGCONFIG};
    }

    public static Object[] convertRegdictionaryArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertRegdictionary(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.REGDICTIONARY};
    }

    public static Object[] convertRegnamespaceArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertRegnamespace(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.REGNAMESPACE};
    }

    public static Object[] convertRegoperArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertRegoper(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.REGOPER};
    }

    public static Object[] convertRegoperatorArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertRegoperator(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.REGOPERATOR};
    }

    public static Object[] convertRegprocArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertRegproc(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.REGPROC};
    }

    public static Object[] convertRegprocedureArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertRegprocedure(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.REGPROCEDURE};
    }

    public static Object[] convertRegroleArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertRegrole(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.REGROLE};
    }

    public static Object[] convertRegtypeArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertRegtype(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.REGTYPE};
    }

    public static Object[] convertJsonArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertJson(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.JSON};
    }

    public static Object[] convertJsonbArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertJsonb(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.JSONB};
    }

    public static Object[] convertJsonpathArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertJsonPath(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.JSONPATH};
    }

    public static Object[] convertMoneyArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGmoney[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertMoney(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.MONEY};
    }

    public static Object[] convertPglsnArray(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new PGobject[arrayLength];
        Object arrayItem, innerValue;
        try {
            for (int i = 0; i < arrayLength; i++) {
                arrayItem = ((BArray) value).get(i);
                innerValue = getArrayValue(arrayItem);
                if (innerValue == null) {
                    arrayData[i] = null;
                } else {
                    arrayData[i] = convertPglsn(innerValue);
                }
            }
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
        return new Object[]{arrayData, Constants.ArrayTypes.PGLSN};
    }

    public static BArray convertPointRecordArray(Object[] dataArray, BArray pointDataArray) throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            pointDataArray.add(i, convertPointToRecord(dataArray[i], Constants.TypeRecordNames.POINTRECORD));
        }
        return pointDataArray;
    }

    public static BArray convertLineRecordArray(Object[] dataArray, BArray lineDataArray) throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            lineDataArray.add(i, convertLineToRecord(dataArray[i], Constants.TypeRecordNames.LINERECORD));
        }
        return lineDataArray;
    }

    public static BArray convertLsegRecordArray(Object[] dataArray, BArray lsegDataArray) throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            lsegDataArray.add(i, convertLsegToRecord(dataArray[i], Constants.TypeRecordNames.LSEGRECORD));
        }
        return lsegDataArray;
    }

    public static BArray convertBoxRecordArray(Object[] dataArray, BArray boxDataArray) throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            boxDataArray.add(i, convertBoxToRecord(dataArray[i], Constants.TypeRecordNames.BOXRECORD));
        }
        return boxDataArray;
    }

    public static BArray convertPathRecordArray(Object[] dataArray, BArray pathDataArray) throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            pathDataArray.add(i, convertPathToRecord(dataArray[i], Constants.TypeRecordNames.PATHRECORD));
        }
        return pathDataArray;
    }

    public static BArray convertPolygonRecordArray(Object[] dataArray, BArray polygonDataArray) throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            polygonDataArray.add(i, convertPolygonToRecord(dataArray[i], Constants.TypeRecordNames.POLYGONRECORD));
        }
        return polygonDataArray;
    }

    public static BArray convertCircleRecordArray(Object[] dataArray, BArray circleDataArray) throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            circleDataArray.add(i, convertCircleToRecord(dataArray[i], Constants.TypeRecordNames.CIRCLERECORD));
        }
        return circleDataArray;
    }

    public static BArray convertIntervalRecordArray(Object[] dataArray, BArray intervalDataArray) 
            throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            intervalDataArray.add(i, convertIntervalToRecord(dataArray[i], Constants.TypeRecordNames.INTERVALRECORD));
        }
        return intervalDataArray;
    }

    public static BArray convertInt4RangeRecordArray(Object[] dataArray, BArray int4rangeDataArray) 
            throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            int4rangeDataArray.add(i, convertInt4rangeToRecord(dataArray[i],
                 Constants.TypeRecordNames.INTEGERRANGERECORD));
        }
        return int4rangeDataArray;
    }

    public static BArray convertInt8RangeRecordArray(Object[] dataArray, BArray int8rangeDataArray) 
            throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            int8rangeDataArray.add(i, convertInt8rangeToRecord(dataArray[i], 
                Constants.TypeRecordNames.LONGRANGERECORD));
        }
        return int8rangeDataArray;
    }

    public static BArray convertNumRangeRecordArray(Object[] dataArray, BArray numrangeDataArray) 
            throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            numrangeDataArray.add(i, convertNumrangeToRecord(dataArray[i], 
                Constants.TypeRecordNames.NUMERICALRANGERECORD));
        }
        return numrangeDataArray;
    }

    public static BArray convertTsRangeRecordArray(Object[] dataArray, BArray tsrangeDataArray) 
            throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            tsrangeDataArray.add(i, converTsrangeToRecord(dataArray[i], 
                Constants.TypeRecordNames.TIMESTAMP_RANGE_RECORD_CIVIL));
        }
        return tsrangeDataArray;
    }

    public static BArray convertTstzRangeRecordArray(Object[] dataArray, BArray tstzrangeDataArray) 
            throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            tstzrangeDataArray.add(i, convertTstzrangeToRecord(dataArray[i], 
                Constants.TypeRecordNames.TIMESTAMPTZ_RANGE_RECORD_CIVIL));
        }
        return tstzrangeDataArray;
    }

    public static BArray convertDateRangeRecordArray(Object[] dataArray, BArray daterangeDataArray) 
            throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            daterangeDataArray.add(i, convertDaterangeToRecord(dataArray[i], 
                Constants.TypeRecordNames.DATERANGE_RECORD_TYPE));
        }
        return daterangeDataArray;
    }
    
    public static BArray convertStringArray(Object[] dataArray, BArray stringDataArray) 
            throws SQLException {
        for (int i = 0; i < dataArray.length; i++) {
            if (dataArray[i] == null) {
                stringDataArray.append(null); 
             } else {
                stringDataArray.append(fromString(dataArray[i].toString()));
             }
        }
        return stringDataArray;
    }

    public static BArray convertJsonArray(Object[] dataArray, BArray jsonDataArray) 
            throws SQLException, ApplicationError {
        for (int i = 0; i < dataArray.length; i++) {
            if (dataArray[i] == null) {
                jsonDataArray.append(null); 
             } else {
                jsonDataArray.append(getJsonValue(dataArray[i]));
             }
        }
        return jsonDataArray;
    }

    private static Object getArrayValue(Object arrayElement) {
        if (arrayElement instanceof BObject) {
            BObject objectValue = (BObject) arrayElement;
            return objectValue.get(Constants.TypedValueFields.VALUE);
        }
        return arrayElement;
    }

    public static Object getJsonValue(Object value) throws SQLException, ApplicationError {
        if (value == null) {
            return null;
        }
        try {
            String jsonString = value.toString();
            return ConversionHelperUtils.getJson(jsonString);
        } catch (SQLException ex) {
            throw new SQLException("Unsupported Value: " + value + " for type: " + "Json");
        } catch (ApplicationError ex) {
            throw new ApplicationError("Unsupported Value: " + value + " for type: " + "Json");
        }
    } 

    private static PGobject setPGobject(String type, String value) throws SQLException {
        try {
            if (value == null) {
                return null;
            }
            PGobject pgobject =  new PGobject();
            pgobject.setType(type);
            pgobject.setValue(value);
            return pgobject;
        } catch (SQLException ex) {
            throw new SQLException("Unsupported Value: " + value + " for type: " + type);
        }
    }
    
    private static PGmoney setPGmoney(double value) {
        PGmoney money;
        money = new PGmoney(value);
        return money;
    }

    private static PGmoney setPGmoney(String value) throws SQLException {
        PGmoney money;
        try {
            money = new PGmoney(value);
        } catch (SQLException ex) {
            throw new SQLException("Unsupported Value: " + value + " for type: " + "money");
        }
        return money;
    }

    public static String getArrayType(java.sql.Array array)  throws ApplicationError {
        try {
            PgArray pgArray =  (PgArray) array;
            return pgArray.getBaseTypeName();
        } catch (SQLException ex) {
            throw new ApplicationError(ex.getMessage());
        }
    }
} 
