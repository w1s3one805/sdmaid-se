package eu.darken.sdmse.systemcleaner.core.filter.stock

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import eu.darken.sdmse.R
import eu.darken.sdmse.common.areas.DataArea
import eu.darken.sdmse.common.ca.CaDrawable
import eu.darken.sdmse.common.ca.CaString
import eu.darken.sdmse.common.ca.toCaDrawable
import eu.darken.sdmse.common.ca.toCaString
import eu.darken.sdmse.common.datastore.value
import eu.darken.sdmse.common.debug.logging.log
import eu.darken.sdmse.common.debug.logging.logTag
import eu.darken.sdmse.common.files.APathLookup
import eu.darken.sdmse.common.files.GatewaySwitch
import eu.darken.sdmse.common.files.segs
import eu.darken.sdmse.systemcleaner.core.SystemCleanerSettings
import eu.darken.sdmse.systemcleaner.core.filter.BaseSystemCleanerFilter
import eu.darken.sdmse.systemcleaner.core.filter.SystemCleanerFilter
import eu.darken.sdmse.systemcleaner.core.filter.toDeletion
import eu.darken.sdmse.systemcleaner.core.sieve.BaseSieve
import eu.darken.sdmse.systemcleaner.core.sieve.SegmentCriterium
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

class LinuxFilesFilter @Inject constructor(
    private val baseSieveFactory: BaseSieve.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseSystemCleanerFilter() {

    override suspend fun getIcon(): CaDrawable = R.drawable.ic_os_linux.toCaDrawable()

    override suspend fun getLabel(): CaString = R.string.systemcleaner_filter_linuxfiles_label.toCaString()

    override suspend fun getDescription(): CaString = R.string.systemcleaner_filter_linuxfiles_summary.toCaString()

    override suspend fun targetAreas(): Set<DataArea.Type> = setOf(
        DataArea.Type.SDCARD,
    )

    private lateinit var sieve: BaseSieve

    override suspend fun initialize() {
        val regexes = setOf(
            Regex("^(?:[\\W\\w]+/\\.Trash)$".replace("/", "\\" + File.separator)),
            Regex("^(?:[\\W\\w]+/\\.Trash-[0-9]{1,4})$".replace("/", "\\" + File.separator)),
        )
        val config = BaseSieve.Config(
            targetTypes = setOf(BaseSieve.TargetType.DIRECTORY),
            areaTypes = targetAreas(),
            pathRegexes = regexes,
            pfpExclusions = setOf(
                SegmentCriterium(segs("Android"), mode = SegmentCriterium.Mode.Ancestor())
            )
        )
        sieve = baseSieveFactory.create(config)
        log(TAG) { "initialized()" }
    }


    override suspend fun match(item: APathLookup<*>): SystemCleanerFilter.Match? {
        return sieve.match(item).toDeletion()
    }

    override suspend fun process(matches: Collection<SystemCleanerFilter.Match>) {
        matches.deleteAll(gatewaySwitch)
    }

    override fun toString(): String = "${this::class.simpleName}(${hashCode()})"

    @Reusable
    class Factory @Inject constructor(
        private val settings: SystemCleanerSettings,
        private val filterProvider: Provider<LinuxFilesFilter>
    ) : SystemCleanerFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterLinuxFilesEnabled.value()
        override suspend fun create(): SystemCleanerFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): SystemCleanerFilter.Factory
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "Filter", "LinuxFiles")
    }
}