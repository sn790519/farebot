package com.codebutler.farebot.card.desfire.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.desfire.DesfireFileSettings;
import com.codebutler.farebot.card.desfire.RecordDesfireFileSettings;
import com.codebutler.farebot.card.desfire.StandardDesfireFileSettings;
import com.codebutler.farebot.card.desfire.ValueDesfireFileSettings;
import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayInputStream;

import static com.codebutler.farebot.card.desfire.DesfireFileSettings.BACKUP_DATA_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.CYCLIC_RECORD_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.LINEAR_RECORD_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.STANDARD_DATA_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.VALUE_FILE;

@AutoValue
public abstract class RawDesfireFileSettings {

    @NonNull
    public static RawDesfireFileSettings create(byte[] data) {
        return new AutoValue_RawDesfireFileSettings(ByteArray.create(data));
    }

    @NonNull
    public static TypeAdapter<RawDesfireFileSettings> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawDesfireFileSettings.GsonTypeAdapter(gson);
    }

    public byte fileType() {
        return data().bytes()[0];
    }

    @NonNull
    public DesfireFileSettings parse() {
        ByteArrayInputStream stream = new ByteArrayInputStream(data().bytes());

        byte fileType = (byte) stream.read();
        byte commSetting = (byte) stream.read();

        byte[] accessRights = new byte[2];
        stream.read(accessRights, 0, accessRights.length);

        switch (fileType) {
            case STANDARD_DATA_FILE:
            case BACKUP_DATA_FILE:
                return createStandardDesfireFileSettings(fileType, commSetting, accessRights, stream);
            case LINEAR_RECORD_FILE:
            case CYCLIC_RECORD_FILE:
                return createRecordDesfireFileSettings(fileType, commSetting, accessRights, stream);
            case VALUE_FILE:
                return createValueDesfireFileSettings(fileType, commSetting, accessRights, stream);
            default:
                throw new RuntimeException("Unknown file type: " + Integer.toHexString(fileType));
        }
    }

    @NonNull
    private StandardDesfireFileSettings createStandardDesfireFileSettings(
            byte fileType,
            byte commSetting,
            byte[] accessRights,
            ByteArrayInputStream stream) {
        byte[] buf = new byte[3];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        int fileSize = Utils.byteArrayToInt(buf);
        return StandardDesfireFileSettings.create(fileType, commSetting, accessRights, fileSize);
    }

    @NonNull
    private RecordDesfireFileSettings createRecordDesfireFileSettings(
            byte fileType,
            byte commSetting,
            byte[] accessRights,
            ByteArrayInputStream stream) {
        byte[] buf = new byte[3];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        int recordSize = Utils.byteArrayToInt(buf);

        buf = new byte[3];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        int maxRecords = Utils.byteArrayToInt(buf);

        buf = new byte[3];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        int curRecords = Utils.byteArrayToInt(buf);

        return RecordDesfireFileSettings.create(
                fileType,
                commSetting,
                accessRights,
                recordSize,
                maxRecords,
                curRecords);
    }

    @NonNull
    private ValueDesfireFileSettings createValueDesfireFileSettings(
            byte fileType,
            byte commSetting,
            byte[] accessRights,
            ByteArrayInputStream stream) {
        byte[] buf = new byte[4];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        int lowerLimit = Utils.byteArrayToInt(buf);

        buf = new byte[4];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        int upperLimit = Utils.byteArrayToInt(buf);

        buf = new byte[4];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        int limitedCreditValue = Utils.byteArrayToInt(buf);

        buf = new byte[1];
        stream.read(buf, 0, buf.length);
        boolean limitedCreditEnabled = buf[0] != 0x00;

        return ValueDesfireFileSettings.create(
                fileType,
                commSetting,
                accessRights,
                lowerLimit,
                upperLimit,
                limitedCreditValue,
                limitedCreditEnabled);
    }

    abstract ByteArray data();
}
