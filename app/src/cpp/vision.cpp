#include <jni.h>
#include <opencv2/opencv.hpp>
#include <GLES2/gl2.h>

extern "C" {

struct TargetData {
    bool found;

    double topWidth;
    double topHeight;

    double topX;
    double topY;

    double bottomWidth;
    double bottomHeight;

    double bottomX;
    double bottomY;
};


cv::Scalar RED(255, 0, 0);
cv::Scalar GREEN(0, 255, 0);
cv::Scalar BLUE(0, 0, 255);

cv::Scalar upperBound(70, 255, 255);
cv::Scalar lowerBound(40,100,40);

static TargetData findTarget(cv::Mat& image) {
    TargetData d;

    static cv::Mat processedImg;

    cv::cvtColor(image, processedImg, CV_BGR2HSV);

    cv::inRange(processedImg, lowerBound, upperBound, processedImg);


    std::vector<std::vector<cv::Point>> contours;
    std::vector<cv::Vec4i> hierarchy;

    cv::erode(processedImg, processedImg, cv::Mat());

    // Uncomment me if you want to see the binary image
    //cv::cvtColor(processedImg, image, CV_GRAY2RGBA);

    cv::findContours(processedImg, contours, hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);

    std::vector<cv::Rect> potentialTargets;

    /*
     * Filter boiler targets
     *
     * Here we just try to filter out any contours that aren't a piece of tape,
     * but matched our color threshold
     */
    for ( int i = 0; i < contours.size(); i++ ) {
        cv::Rect bounding = cv::boundingRect(contours[i]);

        // Hack to ignore reflections from frame bar
        if (bounding.y > 390)
            continue;

        // Drop the target if it's smaller than 200 pixels
        if ( bounding.area() < 200 )
            continue;

        if ( bounding.area() > 2400 )
            continue;

        if ( bounding.height > bounding.width )
            continue;

        // Draw a box around the tape
        cv::rectangle(image, bounding.tl(), bounding.br(), RED, 1);

        potentialTargets.push_back(bounding);
    }

    // If we didn't see at least two pieces of tape, no point in continuing
    if ( potentialTargets.size() < 2 ) {
        d.found = false;
    }
    /*
     * Now we try to match two pieces of tape that make up the boiler
     * i.e two contours stacked, where one is bigger than the other
     */
    else {
        for (int a = 0; a < potentialTargets.size(); a++) {
            for (int b = 0; b < potentialTargets.size(); b++) {
                if (a != b) {
                    cv::Rect top = potentialTargets[a];
                    cv::Rect bottom = potentialTargets[b];

                    // Make sure the top is above the bottom
                    // Skip it if top.y > bottom.y
                    // Note (0,0) is top left
                    if (top.y > bottom.y)
                        continue;

                    // Same X +/- 10 pixels
                    if (abs(top.x - bottom.x) > 20)
                        continue;

                    // Not farther than 100px apart
                    if ( abs(top.y - bottom.y) > 100 )
                        continue;

                    if (abs(top.width - bottom.width) > 20)
                        continue;

                    // assuming we have top and bottom correct...

                    // Box the selected target in green
                    cv::rectangle(image, top.tl(), bottom.br(), GREEN, 1);

                    d.topWidth = top.width;
                    d.topHeight = top.height;

                    d.topX = top.x + (d.topWidth / 2);
                    d.topY = top.y;

                    // Draw top point (used for distance and angle calc)
                    cv::rectangle(image, cv::Point(d.topX, top.y), cv::Point(d.topX, top.y), GREEN, 5);

                    d.bottomWidth = bottom.width;
                    d.bottomHeight = bottom.height;

                    d.bottomX = bottom.x + (d.bottomWidth / 2);
                    d.bottomY = bottom.y;

                    d.found = true;
                }
            }
        }
    }

    return d;
}


bool cFieldsRegistered = false;

static jfieldID cImgPtr;

static jfieldID cFound;

static jfieldID cTopWidth;
static jfieldID cTopHeight;

static jfieldID cTopCentroidX;
static jfieldID cTopCentroidY;

static jfieldID cBottomWidth;
static jfieldID cBottomHeight;

static jfieldID cBottomCentroidX;
static jfieldID cBottomCentroidY;


static void ensureJniRegistered(JNIEnv *env) {
    if ( cFieldsRegistered )
        return;
    cFieldsRegistered = true;

    jclass targetClass = env->FindClass("org/techfire225/firevision2017/Native$Target");

    cImgPtr = env->GetFieldID(targetClass, "imagePtr", "J");

    cFound = env->GetFieldID(targetClass, "found", "Z");

    cTopWidth = env->GetFieldID(targetClass, "topWidth", "D");
    cTopHeight = env->GetFieldID(targetClass, "topHeight", "D");

    cTopCentroidX = env->GetFieldID(targetClass, "topCentroidX", "D");
    cTopCentroidY = env->GetFieldID(targetClass, "topCentroidY", "D");

    cBottomCentroidX = env->GetFieldID(targetClass, "bottomCentroidX", "D");
    cBottomCentroidY = env->GetFieldID(targetClass, "bottomCentroidY", "D");

    cBottomWidth = env->GetFieldID(targetClass, "bottomWidth", "D");
    cBottomHeight = env->GetFieldID(targetClass, "bottomHeight", "D");
}


void processImage(JNIEnv *env, int texOut, int w, int h, jobject targetInfo) {
    ensureJniRegistered(env);

    cv::Mat* input = new cv::Mat();

    input->create(h, w, CV_8UC4);

    glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, input->data);
    cv::flip(*input, *input, 1);

    TargetData target = findTarget(*input);

    env->SetLongField(targetInfo, cImgPtr, (long)input);

    env->SetBooleanField(targetInfo, cFound, target.found);

    env->SetDoubleField(targetInfo, cTopWidth, target.topWidth);
    env->SetDoubleField(targetInfo, cTopHeight, target.topHeight);

    env->SetDoubleField(targetInfo, cTopCentroidX, target.topX);
    env->SetDoubleField(targetInfo, cTopCentroidY, target.topY);

    env->SetDoubleField(targetInfo, cBottomWidth, target.bottomWidth);
    env->SetDoubleField(targetInfo, cBottomHeight, target.bottomHeight);

    env->SetDoubleField(targetInfo, cBottomCentroidX, target.bottomX);
    env->SetDoubleField(targetInfo, cBottomCentroidY, target.bottomY);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texOut);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE,
                    input->data);
}

jbyteArray processJpg(JNIEnv *env, long ptr) {
    cv::Mat* input = (cv::Mat*)ptr;
    static std::vector<uchar> jpgBuffer;
    static std::vector<int> compression_params({CV_IMWRITE_JPEG_QUALITY, 60});

    cv::cvtColor(*input, *input, cv::COLOR_BGR2RGB);
    cv::imencode("test.jpg", *input, jpgBuffer, compression_params);
    jbyteArray jpg = (*env).NewByteArray(jpgBuffer.size());
    if ( jpgBuffer.size() > 0 )
        env->SetByteArrayRegion(jpg, 0, jpgBuffer.size(), (jbyte*)&jpgBuffer[0]);

    input->release();
    delete input;
    return jpg;
}

void releasePtr(long ptr) {
    cv::Mat* realPtr = (cv::Mat*)ptr;
    realPtr->release();
    delete realPtr;
}

}