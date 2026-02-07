package com.app.magkraft.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.MainActivity
import com.app.magkraft.R
import com.app.magkraft.network.ApiClient
import com.app.magkraft.ui.adapters.GroupPopupAdapter
import com.app.magkraft.ui.adapters.LocationPopupAdapter
import com.app.magkraft.ui.model.GroupListModel
import com.app.magkraft.ui.model.LocationListModel
import com.app.magkraft.utils.AuthPref
import com.app.magkraft.utils.EmployeeViewModel
import com.app.magkraft.utils.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.getValue

class SetLocationFragment : Fragment() {


    private var groupId = ""
    private var locationId = ""

    private var groupList = ArrayList<GroupListModel>()
    private var locationList = ArrayList<LocationListModel>()


    private var ctx: Context? = null

    private lateinit var etGroup: EditText
    private lateinit var loader: ProgressBar
    private lateinit var etLocation: EditText
    private lateinit var saveBtn: Button
    var auth: AuthPref? = null

    private val employeeViewModel: EmployeeViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etGroup = view.findViewById(R.id.etGroup)
        etLocation = view.findViewById(R.id.etLocation)
        saveBtn = view.findViewById(R.id.btnSave)
        loader = view.findViewById(R.id.loader)
        auth = AuthPref(ctx!!)

        if (auth?.getLocation("groupName") != "") {
            etGroup.setText(auth?.getLocation("groupName"))
            etLocation.setText(auth?.getLocation("locationName"))
             groupId = auth?.getLocation("groupId").toString()
             locationId = auth?.getLocation("locationId").toString()
        }
        etLocation.setOnClickListener {
            if (groupId.isEmpty()) {
                Toast.makeText(ctx, "Select group First", Toast.LENGTH_SHORT).show()

            } else {
                showLocationPopup(etLocation, locationList) {

                    etLocation.setText(it.Name)
                    locationId = it.Id.toString()
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            getGroups()
        }

        etGroup.setOnClickListener {
            showGroupPopup(etGroup, groupList) {

                etGroup.setText(it.Name)
                groupId = it.Id.toString()
                locationId = ""
                etLocation.setText("")

                CoroutineScope(Dispatchers.Main).launch {
                    getLocationList(groupId.toInt())
                }
            }
        }

        saveBtn.setOnClickListener {
            if (groupId.isEmpty() || locationId.isEmpty()) {

                (ctx as MainActivity).showToast(ctx!!, "Select Above Fields")

            } else {
                auth?.putLocation("groupId", groupId)
                auth?.putLocation("locationId", locationId)
                auth?.putLocation("groupName", etGroup.text.toString())
                auth?.putLocation("locationName", etLocation.text.toString())

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, HomeFragment())
                    .addToBackStack(null)
                    .commit()

//                    lifecycleScope.launch(Dispatchers.IO) {
                employeeViewModel.syncEmployees(showUI = true)

                //  }
            }
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_refresh, menu)

                val filterItem = menu.findItem(R.id.action_refresh)
                filterItem.icon?.setTint(ContextCompat.getColor(requireContext(), R.color.black))


            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_refresh -> {
                        if (auth?.getLocation("groupName") != "") {
                            employeeViewModel.syncEmployees(showUI = true)
                        } else {
                            Toast.makeText(
                                ctx,
                                "Select Group Name and Location Name First",
                                Toast.LENGTH_LONG
                            ).show()

                        }
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)


        // Observe the state from the ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                employeeViewModel.syncState.collect { state ->
                    when (state) {
                        is SyncState.Loading -> {
                            loader.visibility = View.VISIBLE
                            saveBtn.isEnabled = false // Disable button while loading
                        }

                        is SyncState.Success -> {
                            loader.visibility = View.GONE
                            saveBtn.isEnabled = true
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                            // Optional: Navigate to next screen on success
                        }

                        is SyncState.Error -> {
                            loader.visibility = View.GONE
                            saveBtn.isEnabled = true
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }

                        is SyncState.Idle -> {
                            loader.visibility = View.GONE
                        }
                    }
                }
            }

        }
    }

    private fun getGroups() {

//        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.getGroups()

        call.enqueue(object : Callback<List<GroupListModel>> {

            override fun onResponse(
                call: Call<List<GroupListModel>>, response: Response<List<GroupListModel>>
            ) {
//                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    groupList.clear()
                    groupList.addAll(response.body()!!)
//                    adapter.submitList(groupList)

                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<GroupListModel>>, t: Throwable) {
//                (ctx as MainActivity).hideLoader()
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getLocationList(groupId: Int) {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.getLocations()

        call.enqueue(object : Callback<List<LocationListModel>> {

            override fun onResponse(
                call: Call<List<LocationListModel>>, response: Response<List<LocationListModel>>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    locationList.clear()

                    locationList.addAll(response.body()!!.filter { it.GroupId == groupId })


                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<LocationListModel>>, t: Throwable) {
                (ctx as MainActivity).hideLoader()
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showGroupPopup(
        anchor: View, groups: List<GroupListModel>, onSelect: (GroupListModel) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.popup_group_list, null)
        val popup = PopupWindow(
            view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rv = view.findViewById<RecyclerView>(R.id.rvGroups)

        val adapter = GroupPopupAdapter(groups.toMutableList()) {
            onSelect(it)
            popup.dismiss()
        }

        rv.layoutManager = LinearLayoutManager(ctx)
        rv.adapter = adapter

        etSearch.addTextChangedListener { it ->
//            val filtered = groups.filter { g ->
//                g.name.contains(it.toString(), true)
//            }
//            adapter.update(filtered)
        }

        popup.elevation = 12f
        popup.showAsDropDown(anchor)
    }

    private fun showLocationPopup(
        anchor: View, locations: List<LocationListModel>, onSelect: (LocationListModel) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.popup_location_list, null)
        val popup = PopupWindow(
            view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rvLocations = view.findViewById<RecyclerView>(R.id.rvLocations)

        val adapter = LocationPopupAdapter(locations.toMutableList()) {
            onSelect(it)
            popup.dismiss()
        }

        rvLocations.layoutManager = LinearLayoutManager(ctx)
        rvLocations.adapter = adapter

        etSearch.addTextChangedListener { it ->
//            val filtered = groups.filter { g ->
//                g.name.contains(it.toString(), true)
//            }
//            adapter.update(filtered)
        }

        popup.elevation = 12f
        popup.showAsDropDown(anchor)
    }
}