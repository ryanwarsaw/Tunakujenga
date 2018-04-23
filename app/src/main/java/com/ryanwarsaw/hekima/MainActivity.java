package com.ryanwarsaw.hekima;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.google.gson.GsonBuilder;
import com.ryanwarsaw.hekima.adapter.MenuAdapter;
import com.ryanwarsaw.hekima.model.Curriculum;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.Cleanup;
import lombok.Getter;

public class MainActivity extends AppCompatActivity {

  @Getter
  public Curriculum curriculum;
  private MenuAdapter menuAdapter;
  private String[] permissions = {
      permission.WRITE_EXTERNAL_STORAGE,
      permission.READ_EXTERNAL_STORAGE
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Manually update the orientation when initially starting the app.
    onConfigurationChanged(getResources().getConfiguration());

    // Import the content.json file into memory, log the result.
    curriculum = parseContentFile("content.json");

    // Check edge case of the app not having appropriate permissions.
    if (curriculum != null) {
      // Build the main menu options from the content.json file
      menuAdapter = new MenuAdapter(this, curriculum);
      ((ListView) findViewById(R.id.menu_options)).setAdapter(menuAdapter);
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    LinearLayout layout = findViewById(R.id.main_menu);
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      layout.setOrientation(LinearLayout.HORIZONTAL);
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
      layout.setOrientation(LinearLayout.VERTICAL);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    if (requestCode == 1 && grantResults.length > 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      // Successfully received permission approval, finish inflating the main menu.
      curriculum = parseContentFile("content.json");
      menuAdapter = new MenuAdapter(this, curriculum);
      ((ListView) findViewById(R.id.menu_options)).setAdapter(menuAdapter);
    } else {
      finishAffinity(); // Failed to receive permission approval, kill the application.
    }
  }

  /**
   * Parses the content file and builds our internal Curriculum object. By default it will
   * check the Download/ folder on your Android device for a file with the name provided. If
   * it is unable to find that file, it will default to the app packaged version.
   * @param fileName The fully qualified name of the file (including extension).
   * @return Curriculum object representation of the content file, or null if an error occurred.
   */
  private Curriculum parseContentFile(String fileName) {
    if (hasPermissions(this, permissions)) {
      File externalFile = new File(Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + fileName);
      try {
        @Cleanup InputStream inputStream = externalFile.exists() ?
            new FileInputStream(externalFile) : getResources().openRawResource(R.raw.content);
        @Cleanup ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        outputStream.write(buffer);

        return new GsonBuilder().create().fromJson(outputStream.toString(), Curriculum.class);
      } catch (IOException e) {
        throw new RuntimeException("An error occurred while trying to parse file: " + fileName);
      }
    } else {
      ActivityCompat.requestPermissions(this, permissions, 1);
    }
    return null;
  }

  /**
   * Utility method for checking multiple permissions at once for a required operation.
   * @param context The relevant context object currently being used.
   * @param permissions String array of all permissions to check for approval.
   * @return bool Depending on whether all appropriate permissions are present or not.
   */
  private boolean hasPermissions(Context context, String... permissions) {
    if (VERSION.SDK_INT >= VERSION_CODES.M && context != null && permissions != null) {
      for (String permission : permissions) {
        int result = ActivityCompat.checkSelfPermission(context, permission);
        if (result != PackageManager.PERMISSION_GRANTED) return false;
      }
    }
    return true;
  }
}
