package com.example.ml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.odml.image.BitmapMlImageBuilder;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    TextView t;
    ImageView iv;
    Button b, b2;
    ImageLabeler imageLabeler;

    File photoFile;

    ObjectDetector objectDetector;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b = findViewById(R.id.button);
        b2 = findViewById(R.id.button2);
        iv = findViewById(R.id.imageView);
        t = findViewById(R.id.textView);

//        imageLabeler = ImageLabeling.getClient(new ImageLabelerOptions.Builder().setConfidenceThreshold(0.0f).build()); // IMAGE CLASSIFICATION

//        LocalModel localModel = new LocalModel.Builder().setAssetFilePath("model_flowers.tflite").build();
//        CustomImageLabelerOptions options = new CustomImageLabelerOptions.Builder(localModel).setConfidenceThreshold(0.0f).setMaxResultCount(5).build();
//        imageLabeler = ImageLabeling.getClient(options);   // CUSTOM IMAGE (FLOWER) CLASSIFICATION

        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE).enableMultipleObjects().enableClassification().build();
        objectDetector = ObjectDetection.getClient(options); // OBJECT DETECTION

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,1000);
            }
        });

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                photoFile = createPhotoFile();

                Uri fileUri = FileProvider.getUriForFile(MainActivity.this,"com.iago.fileprovider",photoFile);

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT,fileUri);
                startActivityForResult(intent,1001);
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode==RESULT_OK && requestCode==1000){
            Uri uri = data.getData();

            try {
                Bitmap bitmap;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
                }
                else{
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                }
                iv.setImageBitmap(bitmap);
//                Classify(bitmap);
                ClassifyObject(bitmap); // OBJECT DETECTION


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        else{
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            iv.setImageBitmap(bitmap);
//            Classify(bitmap);
            ClassifyObject(bitmap); // OBJECT DETECTION
        }

    }

    public void Classify(Bitmap bitmap){
        InputImage inputImage = InputImage.fromBitmap(bitmap,0);
        imageLabeler.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
            @Override
            public void onSuccess(List<ImageLabel> imageLabels) {
                if(imageLabels.size()>0){
                    StringBuilder builder = new StringBuilder();
                    for (ImageLabel label:imageLabels
                         ) {
                        builder.append(label.getText()).append(" : ").append(label.getConfidence()).append("\n");
                    }
                    t.setText(builder.toString());
                }
                else {
                    t.setText("NOT CLASSIFIABLE");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }
    public File createPhotoFile(){
        String name = null;
        File photoFileDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"ML_IMAGE_HELPER");
        if(!photoFileDir.exists()){
            photoFileDir.mkdirs();
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        }
        File file = new File(photoFileDir.getPath()+File.separator+name);
        return file;
    }

    public void ClassifyObject(Bitmap bitmap){
        InputImage inputImage = InputImage.fromBitmap(bitmap,0);
        objectDetector.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
            @Override
            public void onSuccess(List<DetectedObject> detectedObjects) {
                if(!detectedObjects.isEmpty()){
                    StringBuilder builder = new StringBuilder();
                    List<BoxWithLabel> boxes = new ArrayList<>();
                    for(DetectedObject object : detectedObjects){
                        if(!object.getLabels().isEmpty()){
                            String label = object.getLabels().get(0).getText();
                            builder.append(label).append(": ").append(object.getLabels().get(0).getConfidence()).append("\n");
                            boxes.add(new BoxWithLabel(object.getBoundingBox(),label));
                        }
                        else{
                            builder.append("Unknown").append("\n");
                        }
                    }
                    t.setText(builder.toString());
                    drawDetectionResult(boxes,bitmap);
                }
                else{
                    t.setText("Could not detected");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void drawDetectionResult(List<BoxWithLabel> boxes,Bitmap bitmap){
        Bitmap outmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(outmap);
        Paint penRect = new Paint();
        penRect.setColor(Color.RED);
        penRect.setStyle(Paint.Style.STROKE);
        penRect.setStrokeWidth(8f);

        Paint penLabel = new Paint();
        penLabel.setColor(Color.YELLOW);
        penLabel.setStyle(Paint.Style.FILL_AND_STROKE);
        penLabel.setTextSize(96f);
        penLabel.setStrokeWidth(2f);

        for(BoxWithLabel boxWithLabel : boxes){
            canvas.drawRect(boxWithLabel.rect,penRect);

            Rect labelSize = new Rect(0,0,0,0);
            penLabel.getTextBounds(boxWithLabel.label, 0,boxWithLabel.label.length(),labelSize);
            float fontSize = penLabel.getTextSize() * boxWithLabel.rect.width() / labelSize.width();
            if (fontSize < penLabel.getTextSize()) {
                penLabel.setTextSize(fontSize);
            }

            canvas.drawText(boxWithLabel.label, boxWithLabel.rect.left,boxWithLabel.rect.top+labelSize.height(),penLabel);
        }

        iv.setImageBitmap(outmap);
    }


}