package com.hkh.korealocal114.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hkh.korealocal114.R;

public class EmptyFragment extends StateFragment {
	Context mContext;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContext = getActivity();
		View rootView = inflater.inflate(R.layout.fragment_empty, container, false);
		return rootView;
	}
}
