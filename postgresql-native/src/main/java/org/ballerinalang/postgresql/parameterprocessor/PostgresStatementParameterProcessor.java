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

package org.ballerinalang.postgresql.parameterprocessor;

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXml;
import org.ballerinalang.postgresql.Constants;
import org.ballerinalang.postgresql.utils.ConverterUtils;
import org.ballerinalang.sql.exception.ApplicationError;
import org.ballerinalang.sql.parameterprocessor.DefaultStatementParameterProcessor;
import org.ballerinalang.sql.utils.Utils;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.ballerinalang.stdlib.io.utils.IOConstants;
import org.ballerinalang.stdlib.io.utils.IOUtils;
import org.ballerinalang.stdlib.time.util.TimeValueHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Represent the methods for process SQL statements.
 *
 * @since 0.1.0
 */
public class PostgresStatementParameterProcessor extends DefaultStatementParameterProcessor {
    private static final PostgresStatementParameterProcessor instance = new PostgresStatementParameterProcessor();

     /**
     * Singleton static method that returns an instance of `PostgresStatementParameterProcessor`.
     * @return PostgresStatementParameterProcessor
     */
    public static PostgresStatementParameterProcessor getInstance() {
        return instance;
    }
    @Override
    protected Object[] getDateTimeValueArrayData(Object value) throws ApplicationError {
        return getDateTimeAndTimestampValueArrayData(value);
    }
    @Override
    protected Object[] getTimestampValueArrayData(Object value) throws ApplicationError {
        return getDateTimeAndTimestampValueArrayData(value);
    }

    protected Object[] getTimeValueArrayData(Object value) throws ApplicationError {
        BArray array = (BArray) value;
        int arrayLength = array.size();
        Object innerValue;
        Object[] arrayData = new Object[arrayLength];
        boolean containsTimeZone = false; 
        for (int i = 0; i < arrayLength; i++) {
            innerValue = array.get(i);
            if (innerValue == null) {
                arrayData[i] = null;
            } else if (innerValue instanceof BString) {
                try {
                    arrayData[i] = Time.valueOf(innerValue.toString());
                } catch (java.lang.NumberFormatException ex) {
                    throw new ApplicationError("Unsupported String Value " + innerValue
                            .toString() + " for Time Array");
                }
                // arrayData[i] = innerValue.toString();
            } else if (innerValue instanceof BMap) {
                BMap timeMap = (BMap) innerValue;
                int hour = Math.toIntExact(timeMap.getIntValue(StringUtils.
                        fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_HOUR)));
                int minute = Math.toIntExact(timeMap.getIntValue(StringUtils.
                        fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_MINUTE)));
                BDecimal second = BDecimal.valueOf(0);
                if (timeMap.containsKey(StringUtils
                        .fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_SECOND))) {
                    second = ((BDecimal) timeMap.get(StringUtils
                            .fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_SECOND)));
                }
                int zoneHours = 0;
                int zoneMinutes = 0;
                BDecimal zoneSeconds = BDecimal.valueOf(0);
                boolean timeZone = false;
                if (timeMap.containsKey(StringUtils.
                        fromString(org.ballerinalang.stdlib.time.util.Constants.CIVIL_RECORD_UTC_OFFSET))) {
                    timeZone = true;
                    containsTimeZone = true;
                    BMap zoneMap = (BMap) timeMap.get(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.CIVIL_RECORD_UTC_OFFSET));
                    zoneHours = Math.toIntExact(zoneMap.getIntValue(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_HOUR)));
                    zoneMinutes = Math.toIntExact(zoneMap.getIntValue(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_MINUTE)));
                    if (zoneMap.containsKey(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_SECOND))) {
                        zoneSeconds = ((BDecimal) zoneMap.get(StringUtils.
                                fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_SECOND)));
                    }
                }
                int intSecond = second.decimalValue().setScale(0, RoundingMode.FLOOR).intValue();
                int intNanoSecond = second.decimalValue().subtract(new BigDecimal(intSecond))
                        .multiply(org.ballerinalang.stdlib.time.util.Constants.ANALOG_GIGA)
                        .setScale(0, RoundingMode.HALF_UP).intValue();
                LocalTime localTime = LocalTime.of(hour, minute, intSecond, intNanoSecond);
                if (timeZone) {
                    int intZoneSecond = zoneSeconds.decimalValue().setScale(0, RoundingMode.HALF_UP)
                            .intValue();
                    OffsetTime offsetTime = OffsetTime.of(localTime,
                            ZoneOffset.ofHoursMinutesSeconds(zoneHours, zoneMinutes, intZoneSecond));
                    arrayData[i] = offsetTime;
                } else {
                    arrayData[i] = Time.valueOf(localTime);
                }
            } else {
                throw Utils.throwInvalidParameterError(innerValue, "Time Array");
            }            
        }        
        if (containsTimeZone) {
            return new Object[]{arrayData, Constants.ArrayTypes.TIMETZ};
        } else {
            return new Object[]{arrayData, Constants.ArrayTypes.TIME};
        }
    }

    private Object[] getDateTimeAndTimestampValueArrayData(Object value) throws ApplicationError {
        BArray array = (BArray) value;
        int arrayLength = array.size();
        Object innerValue;
        boolean containsTimeZone = false; 
        Object[] arrayData = new Object[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            innerValue = array.get(i);
            if (innerValue == null) {
                arrayData[i] = null;
            } else if (innerValue instanceof BString) {
                try {
                    java.time.format.DateTimeFormatter formatter = 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    arrayData[i] = LocalDateTime.parse(innerValue.toString(), formatter);
                } catch (java.time.format.DateTimeParseException ex) {
                    throw new ApplicationError("Unsupported String Value " + innerValue
                            .toString() + " for DateTime Array");
                }
            } else if (innerValue instanceof BArray) {
                //this is mapped to time:Utc
                BArray dateTimeStruct = (BArray) innerValue;
                ZonedDateTime zonedDt = TimeValueHandler.createZonedDateTimeFromUtc(dateTimeStruct);
                Timestamp timestamp = new Timestamp(zonedDt.toInstant().toEpochMilli());
                arrayData[i] = timestamp;
            } else if (innerValue instanceof BMap) {
                //this is mapped to time:Civil
                BMap dateMap = (BMap) innerValue;
                int year = Math.toIntExact(dateMap.getIntValue(StringUtils
                        .fromString(org.ballerinalang.stdlib.time.util.Constants.DATE_RECORD_YEAR)));
                int month = Math.toIntExact(dateMap.getIntValue(StringUtils
                        .fromString(org.ballerinalang.stdlib.time.util.Constants.DATE_RECORD_MONTH)));
                int day = Math.toIntExact(dateMap.getIntValue(StringUtils
                        .fromString(org.ballerinalang.stdlib.time.util.Constants.DATE_RECORD_DAY)));
                int hour = Math.toIntExact(dateMap.getIntValue(StringUtils
                        .fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_HOUR)));
                int minute = Math.toIntExact(dateMap.getIntValue(StringUtils.
                        fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_MINUTE)));
                BDecimal second = BDecimal.valueOf(0);
                if (dateMap.containsKey(StringUtils
                        .fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_SECOND))) {
                    second = ((BDecimal) dateMap.get(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_SECOND)));
                }
                int zoneHours = 0;
                int zoneMinutes = 0;
                BDecimal zoneSeconds = BDecimal.valueOf(0);
                boolean timeZone = false;
                if (dateMap.containsKey(StringUtils.
                        fromString(org.ballerinalang.stdlib.time.util.Constants.CIVIL_RECORD_UTC_OFFSET))) {
                    timeZone = true;
                    containsTimeZone = true;
                    BMap zoneMap = (BMap) dateMap.get(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.CIVIL_RECORD_UTC_OFFSET));
                    zoneHours = Math.toIntExact(zoneMap.getIntValue(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_HOUR)));
                    zoneMinutes = Math.toIntExact(zoneMap.getIntValue(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_MINUTE)));
                    if (zoneMap.containsKey(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_SECOND))) {
                        zoneSeconds = ((BDecimal) zoneMap.get(StringUtils.
                                fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_SECOND)));
                    }
                }
                int intSecond = second.decimalValue().setScale(0, RoundingMode.FLOOR).intValue();
                int intNanoSecond = second.decimalValue().subtract(new BigDecimal(intSecond))
                        .multiply(org.ballerinalang.stdlib.time.util.Constants.ANALOG_GIGA)
                        .setScale(0, RoundingMode.HALF_UP).intValue();
                LocalDateTime localDateTime = LocalDateTime
                        .of(year, month, day, hour, minute, intSecond, intNanoSecond);
                if (timeZone) {
                    int intZoneSecond = zoneSeconds.decimalValue().setScale(0, RoundingMode.HALF_UP)
                            .intValue();
                    OffsetDateTime offsetDateTime = OffsetDateTime.of(localDateTime,
                            ZoneOffset.ofHoursMinutesSeconds(zoneHours, zoneMinutes, intZoneSecond));
                    arrayData[i] =  offsetDateTime;
                } else {
                    arrayData[i] = Timestamp.valueOf(localDateTime);
                }
            } else {
                throw Utils.throwInvalidParameterError(value, "TIMESTAMP ARRAY");
            }            
        }        
        if (containsTimeZone) {
            return new Object[]{arrayData, Constants.ArrayTypes.TIMESTAMPTZ};
        } else {
            return new Object[]{arrayData, Constants.ArrayTypes.TIMESTAMP};
        }
    }

    @Override
    protected Object[] getNestedArrayData(Object value) throws ApplicationError {
        Type type = TypeUtils.getType(value);
        Type elementType = ((ArrayType) type).getElementType();
        Type elementTypeOfArrayElement = ((ArrayType) elementType)
                .getElementType();
        if (elementTypeOfArrayElement.getTag() == TypeTags.BYTE_TAG) {
            BArray arrayValue = (BArray) value;
            Object[] arrayData = new byte[arrayValue.size()][];
            for (int i = 0; i < arrayData.length; i++) {
                arrayData[i] = ((BArray) arrayValue.get(i)).getBytes();
            }
            return new Object[]{arrayData, "BYTEA"};
        } else {
            throw Utils.throwInvalidParameterError(value, org.ballerinalang.sql.Constants.SqlTypes.ARRAY);
        }
    }

    @Override
    protected Object[] getDoubleValueArrayData(Object value) throws ApplicationError {
        BArray array = (BArray) value;
        int arrayLength = array.size();
        Object innerValue;
        Object[] arrayData = new Double[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            innerValue = array.get(i);
            if (innerValue == null) {
                arrayData[i] = null;
            } else if (innerValue instanceof Double) {
                arrayData[i] = ((Number) innerValue).doubleValue();
            } else if (innerValue instanceof Long || innerValue instanceof Float || innerValue instanceof Integer) {
                arrayData[i] = ((Number) innerValue).doubleValue();
            } else if (innerValue instanceof BDecimal) {
                arrayData[i] = ((BDecimal) innerValue).decimalValue().doubleValue();
            } else {
                throw Utils.throwInvalidParameterError(innerValue, "Double Array");
            }            
        }        
        return new Object[]{arrayData, "FLOAT4"};
    }

    @Override
    protected Object[] getRealValueArrayData(Object value) throws ApplicationError {
        BArray array = (BArray) value;
        int arrayLength = array.size();
        Object innerValue;
        Object[] arrayData = new Double[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            innerValue = array.get(i);
//            innerValue = objectValue.get(Constants.TypedValueFields.VALUE);
            if (innerValue == null) {
                arrayData[i] = null;
            } else if (innerValue instanceof Double) {
                arrayData[i] = ((Number) innerValue).doubleValue();
            } else if (innerValue instanceof Long || innerValue instanceof Float || innerValue instanceof Integer) {
                arrayData[i] = ((Number) innerValue).doubleValue();
            } else if (innerValue instanceof BDecimal) {
                arrayData[i] = ((BDecimal) innerValue).decimalValue().doubleValue();
            } else {
                throw Utils.throwInvalidParameterError(innerValue, "Real Array");
            }            
        }        
        return new Object[]{arrayData, "FLOAT4"};
    }

    @Override
    public Object[] getBinaryValueArrayData(Object value) throws ApplicationError, IOException {
        BObject objectValue;
        BArray array = (BArray) value;
        int arrayLength = array.size();
        Object innerValue;
        Object[] arrayData = new Object[arrayLength];
        String type = "BYTEA";
        for (int i = 0; i < arrayLength; i++) {
            innerValue = array.get(i);
            if (innerValue == null) {
                arrayData[i] = null;
            } else if (innerValue instanceof BArray) {                
                BArray arrayValue = (BArray) innerValue;
                if (arrayValue.getElementType().getTag() == org.wso2.ballerinalang.compiler.util.TypeTags.BYTE) {
                    arrayData[i] = arrayValue.getBytes();
                } else {
                    throw Utils.throwInvalidParameterError(innerValue, type);
                }
            } else if (innerValue instanceof BObject) {                
                objectValue = (BObject) innerValue;
                if (objectValue.getType().getName().
                        equalsIgnoreCase(org.ballerinalang.sql.Constants.READ_BYTE_CHANNEL_STRUCT) &&
                        objectValue.getType().getPackage().toString()
                            .equalsIgnoreCase(IOUtils.getIOPackage().toString())) {
                    Channel byteChannel = (Channel) objectValue.getNativeData(IOConstants.BYTE_CHANNEL_NAME);
                    arrayData[i] = toByteArray(byteChannel.getInputStream());
                } else {
                    throw Utils.throwInvalidParameterError(innerValue, type + " Array");
                }
            } else {
                throw Utils.throwInvalidParameterError(innerValue, type);
            }            
        }        
        return new Object[]{arrayData, type};
    }

    @Override
    protected Object[] getCustomArrayData(Object value) throws ApplicationError {
        Type type = TypeUtils.getType(value);
        Type elementType = ((ArrayType) type).getElementType();
        int typeTag = elementType.getTag();
        switch (typeTag) {
            case TypeTags.OBJECT_TYPE_TAG:
                if (elementType.getName().equals(Constants.PGTypeNames.POINT)) {
                    return ConverterUtils.convertPointArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.LINE)) {
                    return ConverterUtils.convertLineArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.LSEG)) {
                    return ConverterUtils.convertLsegArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.BOX)) {
                    return ConverterUtils.convertBoxArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.PATH)) {
                    return ConverterUtils.convertPathArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.POLYGON)) {
                    return ConverterUtils.convertPolygonArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.CIRCLE)) {
                    return ConverterUtils.convertCircleArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.INTERVAL)) {
                    return ConverterUtils.convertIntervalArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.INT4RANGE)) {
                    return ConverterUtils.convertInt4RangeArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.INT8RANGE)) {
                    return ConverterUtils.convertInt8RangeArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.NUMRANGE)) {
                    return ConverterUtils.convertNumRangeArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.TSTZRANGE)) {
                    return ConverterUtils.convertTstzRangeArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.TSRANGE)) {
                    return ConverterUtils.convertTsRangeArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.DATERANGE)) {
                    return ConverterUtils.convertDateRangeArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.INET)) {
                    return ConverterUtils.convertInetArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.CIDR)) {
                    return ConverterUtils.convertCidrArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.MACADDR)) {
                    return ConverterUtils.convertMacaddrArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.MACADDR8)) {
                    return ConverterUtils.convertMacaddr8Array(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.UUID)) {
                    return ConverterUtils.convertUuidArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.TSVECTOR)) {
                    return ConverterUtils.convertTsvectotArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.TSQUERY)) {
                    return ConverterUtils.convertTsqueryArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.BITSTRING)) {
                    return ConverterUtils.convertBitstringArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.PGBIT)) {
                    return ConverterUtils.convertBitArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.VARBITSTRING)) {
                    return ConverterUtils.convertVarbitstringArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.XML)) {
                    return ConverterUtils.convertXmlArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.REGCLASS)) {
                    return ConverterUtils.convertRegclassArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.REGCONFIG)) {
                    return ConverterUtils.convertRegconfigArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.REGDICTIONARY)) {
                    return ConverterUtils.convertRegdictionaryArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.REGNAMESPACE)) {
                    return ConverterUtils.convertRegnamespaceArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.REGOPER)) {
                    return ConverterUtils.convertRegoperArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.REG_OPERATOR)) {
                    return ConverterUtils.convertRegoperatorArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.REG_PROC)) {
                    return ConverterUtils.convertRegprocArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.REG_PROCEDURE)) {
                    return ConverterUtils.convertRegprocedureArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.REG_ROLE)) {
                    return ConverterUtils.convertRegroleArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.REG_TYPE)) {
                    return ConverterUtils.convertRegtypeArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.JSON)) {
                    return ConverterUtils.convertJsonArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.JSONB)) {
                    return ConverterUtils.convertJsonbArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.JSON_PATH)) {
                    return ConverterUtils.convertJsonpathArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.MONEY)) {
                    return ConverterUtils.convertMoneyArray(value);
                } else if (elementType.getName().equals(Constants.PGTypeNames.PGLSN)) {
                    return ConverterUtils.convertPglsnArray(value);
                } else {
                    throw new ApplicationError("Unsupported Array type: " + elementType.getName());
                }
            case TypeTags.RECORD_TYPE_TAG:
                if (elementType.getName().equals(Constants.TypeRecordNames.POINTRECORD)) {
                    return ConverterUtils.convertPointArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.LINERECORD)) {
                    return ConverterUtils.convertLineArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.LSEGRECORD)) {
                    return ConverterUtils.convertLsegArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.BOXRECORD)) {
                    return ConverterUtils.convertBoxArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.PATHRECORD)) {
                    return ConverterUtils.convertPathArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.POLYGONRECORD)) {
                    return ConverterUtils.convertPolygonArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.CIRCLERECORD)) {
                    return ConverterUtils.convertCircleArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.INTERVALRECORD)) {
                    return ConverterUtils.convertIntervalArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.INTEGERRANGERECORD)) {
                    return ConverterUtils.convertInt4RangeArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.LONGRANGERECORD)) {
                    return ConverterUtils.convertInt8RangeArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.NUMERICALRANGERECORD)) {
                    return ConverterUtils.convertNumRangeArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.TIMESTAMPTZRANGERECORD)
                    || elementType.getName().equals(Constants.TypeRecordNames.TIMESTAMPTZ_RANGE_RECORD_CIVIL)) {
                    return ConverterUtils.convertTstzRangeArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.TIMESTAMPRANGERECORD)
                || elementType.getName().equals(Constants.TypeRecordNames.TIMESTAMP_RANGE_RECORD_CIVIL)) {
                    return ConverterUtils.convertTsRangeArray(value);
                } else if (elementType.getName().equals(Constants.TypeRecordNames.DATERANGERECORD)
                || elementType.getName().equals(Constants.TypeRecordNames.DATERANGE_RECORD_TYPE)) {
                    return ConverterUtils.convertDateRangeArray(value);
                } else {
                    throw new ApplicationError("Unsupported Array type: " + elementType.getName());
                }
            default:
                throw new ApplicationError("Unsupported Array type: " + elementType.getName());
        }
    }

    @Override
    public int getCustomOutParameterType(BObject typedValue) throws ApplicationError { 
        String sqlType = typedValue.getType().getName();
        switch (sqlType) {
            case Constants.OutParameterNames.PGBIT:
                return Types.BIT;
            case Constants.OutParameterNames.XML:
                return Types.SQLXML;
            case Constants.OutParameterNames.BINARY:
                return Types.BINARY;
            case Constants.OutParameterNames.MONEY:
                return Types.DOUBLE;
            case Constants.OutParameterNames.ENUM:
                return Types.VARCHAR;
            case Constants.OutParameterNames.INET:
            case Constants.OutParameterNames.CIDR:
            case Constants.OutParameterNames.MACADDR:
            case Constants.OutParameterNames.MACADDR8:
            case Constants.OutParameterNames.POINT:
            case Constants.OutParameterNames.LINE:
            case Constants.OutParameterNames.LSEG:
            case Constants.OutParameterNames.POLYGON:
            case Constants.OutParameterNames.PATH:
            case Constants.OutParameterNames.CIRCLE:
            case Constants.OutParameterNames.BOX:
            case Constants.OutParameterNames.UUID:
            case Constants.OutParameterNames.TSVECTOR:
            case Constants.OutParameterNames.TSQUERY:
            case Constants.OutParameterNames.JSON:
            case Constants.OutParameterNames.JSONB:
            case Constants.OutParameterNames.JSONPATH:
            case Constants.OutParameterNames.INTERVAL:
            case Constants.OutParameterNames.INT4RANGE:
            case Constants.OutParameterNames.INT8RANGE:
            case Constants.OutParameterNames.NUMRANGE:
            case Constants.OutParameterNames.TSRANGE:
            case Constants.OutParameterNames.TSTZRANGE:
            case Constants.OutParameterNames.DATERANGE:
            case Constants.OutParameterNames.VARBITSTRING:
            case Constants.OutParameterNames.BITSTRING:
            case Constants.OutParameterNames.PGLSN:
            case Constants.OutParameterNames.REGCLASS:
            case Constants.OutParameterNames.REGCONFIG:
            case Constants.OutParameterNames.REGDICTIONARY:
            case Constants.OutParameterNames.REGNAMESPACE:
            case Constants.OutParameterNames.REGOPER:
            case Constants.OutParameterNames.REGOPERATOR:
            case Constants.OutParameterNames.REGPROC:
            case Constants.OutParameterNames.REGPROCEDURE:
            case Constants.OutParameterNames.REGROLE:
            case Constants.OutParameterNames.REGTYPE:
                return Types.OTHER;
            default:
                throw new ApplicationError("Unsupported OutParameter type: " + sqlType);
        }
    }

    @Override
    protected int getCustomSQLType(BObject typedValue) throws ApplicationError {
        String sqlType = typedValue.getType().getName();
        switch (sqlType) {
            case Constants.PGTypeNames.PGBIT:
                return Types.BIT;
            case Constants.PGTypeNames.XML:
                return Types.SQLXML;
            case Constants.PGTypeNames.MONEY:
                return Types.DOUBLE;
            case Constants.PGTypeNames.ENUM:
                return Types.VARCHAR;
            case Constants.PGTypeNames.INET:
            case Constants.PGTypeNames.CIDR:
            case Constants.PGTypeNames.MACADDR:
            case Constants.PGTypeNames.MACADDR8:
            case Constants.PGTypeNames.POINT:
            case Constants.PGTypeNames.LINE:
            case Constants.PGTypeNames.LSEG:
            case Constants.PGTypeNames.POLYGON:
            case Constants.PGTypeNames.PATH:
            case Constants.PGTypeNames.CIRCLE:
            case Constants.PGTypeNames.BOX:
            case Constants.PGTypeNames.UUID:
            case Constants.PGTypeNames.TSVECTOR:
            case Constants.PGTypeNames.TSQUERY:
            case Constants.PGTypeNames.JSON:
            case Constants.PGTypeNames.JSONB:
            case Constants.PGTypeNames.JSON_PATH:
            case Constants.PGTypeNames.INTERVAL:
            case Constants.PGTypeNames.INT4RANGE:
            case Constants.PGTypeNames.INT8RANGE:
            case Constants.PGTypeNames.NUMRANGE:
            case Constants.PGTypeNames.TSRANGE:
            case Constants.PGTypeNames.TSTZRANGE:
            case Constants.PGTypeNames.DATERANGE:
            case Constants.PGTypeNames.VARBITSTRING:
            case Constants.PGTypeNames.BITSTRING:
            case Constants.PGTypeNames.PGLSN:
            case Constants.PGTypeNames.REGCLASS:
            case Constants.PGTypeNames.REGCONFIG:
            case Constants.PGTypeNames.REGDICTIONARY:
            case Constants.PGTypeNames.REGNAMESPACE:
            case Constants.PGTypeNames.REGOPER:
            case Constants.PGTypeNames.REG_OPERATOR:
            case Constants.PGTypeNames.REG_PROC:
            case Constants.PGTypeNames.REG_PROCEDURE:
            case Constants.PGTypeNames.REG_ROLE:
            case Constants.PGTypeNames.REG_TYPE:
            case Constants.PGTypeNames.CUSTOM_TYPES:
                return Types.OTHER;
            default:
                throw new ApplicationError("Unsupported OutParameter type: " + sqlType);
        }
    }

    @Override
    protected void setCustomSqlTypedParam(Connection connection, PreparedStatement preparedStatement,
                    int index, BObject typedValue) throws SQLException, ApplicationError, IOException {
        String sqlType = typedValue.getType().getName();
        Object value = typedValue.get(org.ballerinalang.sql.Constants.TypedValueFields.VALUE);
        switch (sqlType) {
            case Constants.PGTypeNames.INET:
                setInet(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.INET_ARRAY:
                setInetArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.CIDR:
                setCidr(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.CIDR_ARRAY:
                setCidrArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.MACADDR:
                setMacaddr(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.MACADDR_ARRAY:
                setMacaddrArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.MACADDR8_ARRAY:
                setMacaddr8Array(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.MACADDR8:
                setMaacadr8(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.POINT:
                setPoint(preparedStatement, index, value);
                break;
            case Constants.ArrayTypes.POINT_ARRAY_VALUE:
                setPointArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.LINE:
                setLine(preparedStatement, index, value);
                break;
            case Constants.ArrayTypes.LINE_ARRAY_VALUE:
                setLineArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.LSEG:
                setLseg(preparedStatement, index, value);
                break;
            case Constants.ArrayTypes.LSEG_ARRAY_VALUE:
                setLsegArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.PATH:
                setPath(preparedStatement, index, value);
                break;
            case Constants.ArrayTypes.PATH_ARRAY_VALUE:
                setPathArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.POLYGON:
                setPolygon(preparedStatement, index, value);
                break;
            case Constants.ArrayTypes.POLYGON_ARRAY_VALUE:
                setLPolygonArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.CIRCLE:
                setCircle(preparedStatement, index, value);
                break;
            case Constants.ArrayTypes.CIRCLE_ARRAY_VALUE:
                setCircleArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.BOX:
                setBox(preparedStatement, index, value);
                break;
            case Constants.ArrayTypes.BOX_ARRAY_VALUE:
                setBoxArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.UUID:
                setUuid(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.UUID_ARRAY:
                setUuidArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.TSVECTOR:
                setTsvector(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.TSVECTOR_ARRAY:
                setTsvectorArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.TSQUERY:
                setTsquery(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.TSQUERY_ARRAY:
                setTsqueryArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.JSON:
                setJson(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.JSON_ARRAY:
                setJsonArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.JSONB:
                setJsonb(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.JSONB_ARRAY:
                setJsonBArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.JSON_PATH:
                setJsonpath(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.JSON_PATH_ARRAY:
                setJsonPathArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.INTERVAL:
                setInterval(preparedStatement, index, value);
                break;
            case Constants.ArrayTypes.INTERVAL_ARRAY:
                setIntervalArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.INT4RANGE:
                setInt4Range(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.INTEGER_RANGE_ARRAY:
                setIntegerRangeArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.INT8RANGE:
                setInt8Range(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.LONG_RANGE_ARRAY:
                setLongRangeArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.NUMRANGE:
                setNumRange(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.NUM_RANGE_ARRAY:
                setNumRangeArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.TSRANGE:
                setTsRange(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.TIME_STAMP_RANGE_ARRAY:
                setTimeStampRangeArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.TSTZRANGE:
                setTstzRange(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.TIME_STAMP_Z_RANGE_ARRAY:
                setTimeStampZRangeArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.DATERANGE:
                setDateRange(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.DATE_RANGE_ARRAY:
                setDateRangeArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.PGBIT:
                setPGBit(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.PG_BIT_ARRAY:
                setPGBitArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.VARBITSTRING:
                setVarBitString(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.VAR_BIT_STRING_ARRAY:
                setVarBitStringArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.BITSTRING:
                setBitString(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.BIT_STRING_ARRAY:
                setBitStringArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.PGLSN:
                setPglsn(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.PGLSN_ARRAY:
                setPglsnArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.MONEY:
                setMoney(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.MONEY_ARRAY:
                setMoneyArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REGCLASS:
                setRegclass(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_CLASS_ARRAY:
                setRegClassArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REGCONFIG:
                setRegconfig(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_CONFIG_ARRAY:
                setRegConfigArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REGDICTIONARY:
                setRegdictionary(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_DICTIONARY_ARRAY:
                setRegDictionaryArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REGNAMESPACE:
                setRegnamespace(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_NAME_SPACE_ARRAY:
                setRegNamespaceArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REGOPER:
                setRegoper(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_OPER_ARRAY:
                setRegOperArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_OPERATOR:
                setRegoperator(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_OPERATOR_ARRAY:
                setRegOperatorArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_PROC_ARRAY:
                setRegProcArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_PROC:
                setRegproc(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_PROCEDURE:
                setRegprocedure(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_PROCEDURE_ARRAY:
                setRegProcedureArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_ROLE:
                setRegrole(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_ROLE_ARRAY:
                setRegRoleArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_TYPE:
                setRegtype(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.REG_TYPE_ARRAY:
                setRegTypeArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.XML:
                setXmlValue(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.XML_ARRAY:
                setXmlValueArray(connection, preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.CUSTOM_TYPES:
                setCustomType(preparedStatement, index, value);
                break;
            case Constants.PGTypeNames.ENUM:
                setEnum(preparedStatement, index, value);
                break;
            default:
                throw new ApplicationError("Unsupported SQL type: " + sqlType);
        }
    }

    @Override
    protected void setTime(PreparedStatement preparedStatement, String sqlType, int index, Object value)
            throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setTime(index, null);
        } else {
            if (value instanceof BString) {
                preparedStatement.setString(index, value.toString());
            } else if (value instanceof BMap) {
                BMap timeMap = (BMap) value;
                int hour = Math.toIntExact(timeMap.getIntValue(StringUtils.
                        fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_HOUR)));
                int minute = Math.toIntExact(timeMap.getIntValue(StringUtils.
                        fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_MINUTE)));
                BDecimal second = BDecimal.valueOf(0);
                if (timeMap.containsKey(StringUtils
                        .fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_SECOND))) {
                    second = ((BDecimal) timeMap.get(StringUtils
                            .fromString(org.ballerinalang.stdlib.time.util.Constants.TIME_OF_DAY_RECORD_SECOND)));
                }
                int zoneHours = 0;
                int zoneMinutes = 0;
                BDecimal zoneSeconds = BDecimal.valueOf(0);
                boolean timeZone = false;
                if (timeMap.containsKey(StringUtils.
                        fromString(org.ballerinalang.stdlib.time.util.Constants.CIVIL_RECORD_UTC_OFFSET))) {
                    timeZone = true;
                    BMap zoneMap = (BMap) timeMap.get(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.CIVIL_RECORD_UTC_OFFSET));
                    zoneHours = Math.toIntExact(zoneMap.getIntValue(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_HOUR)));
                    zoneMinutes = Math.toIntExact(zoneMap.getIntValue(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_MINUTE)));
                    if (zoneMap.containsKey(StringUtils.
                            fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_SECOND))) {
                        zoneSeconds = ((BDecimal) zoneMap.get(StringUtils.
                                fromString(org.ballerinalang.stdlib.time.util.Constants.ZONE_OFFSET_RECORD_SECOND)));
                    }
                }
                int intSecond = second.decimalValue().setScale(0, RoundingMode.FLOOR).intValue();
                int intNanoSecond = second.decimalValue().subtract(new BigDecimal(intSecond))
                        .multiply(org.ballerinalang.stdlib.time.util.Constants.ANALOG_GIGA)
                        .setScale(0, RoundingMode.HALF_UP).intValue();
                LocalTime localTime = LocalTime.of(hour, minute, intSecond, intNanoSecond);
                if (timeZone) {
                    int intZoneSecond = zoneSeconds.decimalValue().setScale(0, RoundingMode.HALF_UP)
                            .intValue();
                    OffsetTime offsetTime = OffsetTime.of(localTime,
                            ZoneOffset.ofHoursMinutesSeconds(zoneHours, zoneMinutes, intZoneSecond));
                    Object timeObject = ConverterUtils.convertTimetz(offsetTime);
                    preparedStatement.setObject(index, timeObject);
                } else {
                    preparedStatement.setTime(index, Time.valueOf(localTime));
                }
            } else {
                throw Utils.throwInvalidParameterError(value, sqlType);
            }
        }
    }
    
    @Override
    protected void setReal(PreparedStatement preparedStatement, String sqlType, int index, Object value)
            throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setNull(index, Types.REAL);
        } else if (value instanceof Double || value instanceof Long ||
                value instanceof Float || value instanceof Integer) {
            preparedStatement.setFloat(index, ((Number) value).floatValue());
        } else if (value instanceof BDecimal) {
            preparedStatement.setFloat(index, ((BDecimal) value).decimalValue().floatValue());
        } else {
            throw Utils.throwInvalidParameterError(value, sqlType);
        }
    }

    @Override
    protected void setChar(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.CHAR);
        } else {
            preparedStatement.setString(index, value.toString());
        }
    }

    private void setInet(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertInet(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setCidr(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertCidr(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setMacaddr(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertMac(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setMaacadr8(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertMac8(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setPoint(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertPoint(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setLine(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertLine(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setLseg(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertLseg(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setPath(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertPath(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setPolygon(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertPolygon(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setCircle(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertCircle(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setBox(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertBox(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setUuid(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertUuid(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setTsvector(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertTsVector(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setTsquery(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertTsQuery(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setJson(PreparedStatement preparedStatement, int index, Object value)
    throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertJson(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setJsonb(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertJsonb(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setJsonpath(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertJsonPath(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setInterval(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertInterval(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setInt4Range(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertInt4Range(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setInt8Range(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertInt8Range(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setNumRange(PreparedStatement preparedStatement, int index, Object value)
    throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertNumRange(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setTsRange(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertTsRange(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setTstzRange(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertTstzRange(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setDateRange(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertDateRange(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setPGBit(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertBit(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setVarBitString(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertVarbit(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setBitString(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertBitn(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setPglsn(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertPglsn(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setMoney(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertMoney(value);
            preparedStatement.setObject(index, object);
        }
    }  
    
    private void setRegclass(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertRegclass(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setRegconfig(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertRegconfig(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setRegdictionary(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertRegdictionary(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setRegnamespace(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertRegnamespace(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setRegoper(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertRegoper(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setRegoperator(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertRegoperator(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setRegproc(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertRegproc(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setRegprocedure(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertRegprocedure(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setRegrole(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertRegrole(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setRegtype(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertRegtype(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setXmlValue(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertXml(value);
            preparedStatement.setObject(index, object);
        }
    }

    @Override
    protected void setBit(PreparedStatement preparedStatement, String sqlType, int index, Object value)
            throws SQLException, ApplicationError {
        super.setBit(preparedStatement, sqlType, index, value);
    }

    private void setCustomType(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertCustomType(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setEnum(PreparedStatement preparedStatement, int index, Object value)
        throws SQLException, ApplicationError {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertEnum(value);
            preparedStatement.setObject(index, object);
        }
    }

    @Override
    protected void setXml(PreparedStatement preparedStatement, int index, BXml value) throws SQLException {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else {
            Object object = ConverterUtils.convertXml(value);
            preparedStatement.setObject(index, object);
        }
    }

    private void setPointArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertPointArray(value));
    }

    private void setLineArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertLineArray(value));
    }

    private void setLsegArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertLsegArray(value));
    }

    private void setPathArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertPathArray(value));
    }

    private void setLPolygonArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertPolygonArray(value));
    }

    private void setBoxArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertBoxArray(value));
    }

    private void setCircleArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertCircleArray(value));
    }

    private void setIntervalArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertIntervalArray(value));
    }

    private void setIntegerRangeArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertInt4RangeArray(value));
    }

    private void setLongRangeArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertInt8RangeArray(value));
    }

    private void setNumRangeArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertNumRangeArray(value));
    }

    private void setTimeStampZRangeArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertTstzRangeArray(value));
    }

    private void setTimeStampRangeArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertTsRangeArray(value));
    }

    private void setDateRangeArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertDateRangeArray(value));
    }

    private void setInetArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertInetArray(value));
    }

    private void setCidrArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertCidrArray(value));
    }

    private void setMacaddrArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertMacaddrArray(value));
    }

    private void setMacaddr8Array(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertMacaddr8Array(value));
    }

    private void setUuidArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertUuidArray(value));
    }

    private void setTsvectorArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertTsvectotArray(value));
    }

    private void setTsqueryArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertTsqueryArray(value));
    }

    private void setVarBitStringArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertVarbitstringArray(value));
    }

    private void setBitStringArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertBitstringArray(value));
    }

    private void setPGBitArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertBitArray(value));
    }

    private void setRegClassArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertRegclassArray(value));
    }

    private void setRegConfigArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertRegconfigArray(value));
    }

    private void setRegDictionaryArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertRegdictionaryArray(value));
    }

    private void setRegNamespaceArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertRegnamespaceArray(value));
    }

    private void setRegOperArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertRegoperArray(value));
    }

    private void setRegOperatorArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertRegoperatorArray(value));
    }

    private void setRegProcArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertRegprocArray(value));
    }

    private void setRegProcedureArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertRegprocedureArray(value));
    }

    private void setRegRoleArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertRegroleArray(value));
    }

    private void setRegTypeArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertRegtypeArray(value));
    }

    private void setXmlValueArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertXmlArray(value));
    }

    private void setJsonArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertJsonArray(value));
    }

    private void setJsonBArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertJsonbArray(value));
    }

    private void setJsonPathArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertJsonpathArray(value));
    }

    private void setPglsnArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertPglsnArray(value));
    }

    private void setMoneyArray(Connection conn, PreparedStatement preparedStatement, int index, Object value)
            throws SQLException, ApplicationError {
        setPreparedStatement(conn, preparedStatement, index, ConverterUtils.convertMoneyArray(value));
    }

    private void setPreparedStatement(Connection conn, PreparedStatement preparedStatement, int index,
                                      Object[] arrayData) throws SQLException {
        if (arrayData[0] != null) {
            Array array = conn.createArrayOf((String) arrayData[1], (Object[]) arrayData[0]);
            preparedStatement.setArray(index, array);
        } else {
            preparedStatement.setArray(index, null);
        }
    }
}
