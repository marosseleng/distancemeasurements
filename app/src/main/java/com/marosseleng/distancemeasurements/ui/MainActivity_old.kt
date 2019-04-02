package com.marosseleng.distancemeasurements.ui

/**
 * @author Maroš Šeleng
 */


//        bottomNavigation.setOnNavigationItemSelectedListener(this)
//        if (savedInstanceState == null) {
//            showTopLevelFragment(MeasurementsFragment.FRAGMENT_TAG, false) {
//                MeasurementsFragment.newInstance()
//            }
//        }

//    override fun onNavigationItemSelected(item: MenuItem): Boolean {
//        val newItemId = item.itemId
//        val oldItemId = bottomNavigation.selectedItemId
//        val forceRecreation = newItemId == oldItemId
//        when (newItemId) {
//            R.id.measurements -> {
//                showTopLevelFragment(MeasurementsFragment.FRAGMENT_TAG, forceRecreation) {
//                    MeasurementsFragment.newInstance()
//                }
//            }
//            R.id.newMeasurement -> {
//                showTopLevelFragment(NewMeasurementFragment.FRAGMENT_TAG, forceRecreation) {
//                    NewMeasurementFragment.newInstance()
//                }
//            }
//            R.id.thirdItem -> {
//
//            }
//        }
//        return true
//    }

//    private fun showTopLevelFragment(tag: String, forceRecreate: Boolean, fragmentFactory: () -> Fragment) {
////        val fragmentTransaction = supportFragmentManager.beginTransaction()
////
////        val curFrag = supportFragmentManager.primaryNavigationFragment
////        if (curFrag != null) {
////            fragmentTransaction.detach(curFrag)
////        }
////
////        var fragment = supportFragmentManager.findFragmentByTag(tag)
////        if (fragment == null) {
////            // Fragment not present in the FM
////            fragment = fragmentFactory()
////            fragmentTransaction.add(container.id, fragment, tag)
////        } else {
////            if (forceRecreate) {
////                // Fragment is present in the FM, but recreating it
////                fragmentTransaction.remove(fragment)
////                fragment = fragmentFactory()
////                fragmentTransaction.add(container.id, fragment, tag)
////            } else {
////                // Fragment is present in the FM, reusing it
////                fragmentTransaction.attach(fragment)
////            }
////        }
////
////        fragmentTransaction.setPrimaryNavigationFragment(fragment)
////        fragmentTransaction.setReorderingAllowed(true)
////        fragmentTransaction.commit()
//    }