package com.marosseleng.distancemeasurements.ui.newmeasurement

/**
 * @author Maroš Šeleng
 */

interface MeasurementFinished
interface SavingFinished

sealed class MeasurementProgress {
    object NotStarted : MeasurementProgress()
    object Started : MeasurementProgress()
    object Saving : MeasurementProgress(), MeasurementFinished
    object Saved : MeasurementProgress(), MeasurementFinished, SavingFinished
    object NotSaved : MeasurementProgress(), MeasurementFinished, SavingFinished
}