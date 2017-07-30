package org.techfire225.firevision2017;

public class Native {

    public static native long processImage(int texOut,
                                    int w,
                                    int h,
                                    Target targetInfo);

    public static native byte[] processJpg(long ptr);
    public static native void releaseImage(long ptr);


    public static class Target {
        public long imagePtr;

        public boolean found;

        public double topCentroidX;
        public double topCentroidY;
        public double topWidth;
        public double topHeight;

        public double bottomCentroidX;
        public double bottomCentroidY;
        public double bottomWidth;
        public double bottomHeight;
    }
}
