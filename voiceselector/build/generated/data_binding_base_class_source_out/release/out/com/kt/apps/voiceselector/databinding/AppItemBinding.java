// Generated by view binder compiler. Do not edit!
package com.kt.apps.voiceselector.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.kt.apps.voiceselector.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class AppItemBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final ShapeableImageView appIcon;

  @NonNull
  public final LinearLayout descContainer;

  @NonNull
  public final MaterialTextView description;

  @NonNull
  public final MaterialTextView title;

  private AppItemBinding(@NonNull ConstraintLayout rootView, @NonNull ShapeableImageView appIcon,
      @NonNull LinearLayout descContainer, @NonNull MaterialTextView description,
      @NonNull MaterialTextView title) {
    this.rootView = rootView;
    this.appIcon = appIcon;
    this.descContainer = descContainer;
    this.description = description;
    this.title = title;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static AppItemBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static AppItemBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
      boolean attachToParent) {
    View root = inflater.inflate(R.layout.app_item, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static AppItemBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.app_icon;
      ShapeableImageView appIcon = ViewBindings.findChildViewById(rootView, id);
      if (appIcon == null) {
        break missingId;
      }

      id = R.id.desc_container;
      LinearLayout descContainer = ViewBindings.findChildViewById(rootView, id);
      if (descContainer == null) {
        break missingId;
      }

      id = R.id.description;
      MaterialTextView description = ViewBindings.findChildViewById(rootView, id);
      if (description == null) {
        break missingId;
      }

      id = R.id.title;
      MaterialTextView title = ViewBindings.findChildViewById(rootView, id);
      if (title == null) {
        break missingId;
      }

      return new AppItemBinding((ConstraintLayout) rootView, appIcon, descContainer, description,
          title);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
