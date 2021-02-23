package com.sdpdigital.glassblockbar.view;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class AnimationChildViewGroups {

    public static class SeekBarInput implements Parcelable {
        public String title;
        public int seekBarProgress;
        public int progressStart;
        public int progressEnd;

        public SeekBarInput(int startProgress, String seekBarTitle, int progressStart, int progressEnd) {
            seekBarProgress = startProgress;
            this.title = seekBarTitle;
            this.progressStart = progressStart;
            this.progressEnd = progressEnd;
        }

        protected SeekBarInput(Parcel in) {
            seekBarProgress = in.readInt();
            title = in.readString();
            progressStart = in.readInt();
            progressEnd = in.readInt();
        }

        public final Creator<SeekBarInput> CREATOR = new Creator<SeekBarInput>() {
            @Override
            public SeekBarInput createFromParcel(Parcel in) {
                return new SeekBarInput(in);
            }

            @Override
            public SeekBarInput[] newArray(int size) {
                return new SeekBarInput[size];
            }
        };

        @Override
        public int describeContents() {
            return super.hashCode();
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(seekBarProgress);
            parcel.writeString(title);
            parcel.writeInt(progressStart);
            parcel.writeInt(progressEnd);
        }
    }

    public static class PatternSelectorInput implements Parcelable {
        public String[] patterns;
        public int patternIdx;

        public PatternSelectorInput(String[] patternArray, int startIdx) {
            patterns = patternArray;
            patternIdx = startIdx;
        }

        protected PatternSelectorInput(Parcel in) {
            in.readStringArray(patterns);
            patternIdx = in.readInt();
        }

        public final Creator<SeekBarInput> CREATOR = new Creator<SeekBarInput>() {
            @Override
            public SeekBarInput createFromParcel(Parcel in) {
                return new SeekBarInput(in);
            }

            @Override
            public SeekBarInput[] newArray(int size) {
                return new SeekBarInput[size];
            }
        };

        @Override
        public int describeContents() {
            return super.hashCode();
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeStringArray(patterns);
            parcel.writeInt(patternIdx);
        }
    }

    public static class ColorGroupPickerInput implements Parcelable {
        public int colorIdx;

        public ColorGroupPickerInput(int colorIdx) {
            this.colorIdx = colorIdx;
        }

        protected ColorGroupPickerInput(Parcel in) {
            colorIdx = in.readInt();
        }

        public final Creator<ColorGroupPickerInput> CREATOR = new Creator<ColorGroupPickerInput>() {
            @Override
            public ColorGroupPickerInput createFromParcel(Parcel in) {
                return new ColorGroupPickerInput(in);
            }

            @Override
            public ColorGroupPickerInput[] newArray(int size) {
                return new ColorGroupPickerInput[size];
            }
        };

        @Override
        public int describeContents() {
            return super.hashCode();
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(colorIdx);
        }
    }
}
