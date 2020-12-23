package com.sdpdigital.glassblockbar.view;

import android.os.Parcel;
import android.os.Parcelable;

public class AnimationChildViewGroups {

    public static class SeekBarInput implements Parcelable {
        public String title;
        public int seekBarProgress;

        public SeekBarInput(int startProgress, String seekBarTitle) {
            seekBarProgress = startProgress;
            this.title = seekBarTitle;
        }

        protected SeekBarInput(Parcel in) {
            seekBarProgress = in.readInt();
            title = in.readString();
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
}
