/*******************************************************************************
 * Copyright (c) 2020, 2022 ArSysOp
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     ArSysOp - initial API and implementation
 *     IILS mbH (Hannes Wellmann) - rewrite to use OSHI 6 and to compute properties lazy
 *******************************************************************************/
package org.eclipse.passage.lic.oshi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.passage.lic.api.inspection.EnvironmentProperty;
import org.eclipse.passage.lic.internal.base.inspection.hardware.Disk;
import org.eclipse.passage.lic.internal.base.inspection.hardware.NetworkInterface;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;

/**
 * Pack of sibling entities in a hardware description
 */
abstract class Swath<T> {

	private final Map<EnvironmentProperty, Function<T, String>> propertyGetters;
	private final Supplier<List<T>> sources;

	Swath(SystemInfo info, Map<EnvironmentProperty, Function<T, String>> propertyGetters) {
		this.sources = (() -> getSource(info)); // Don't Memorize sources. E.g. drives may change over time
		this.propertyGetters = propertyGetters;
	}

	String family() {
		return propertyGetters.keySet().iterator().next().family();
	}

	boolean hasProperty(EnvironmentProperty property) {
		return propertyGetters.containsKey(property);
	}

	Stream<String> valuesFor(EnvironmentProperty property) {
		Function<T, String> getter = propertyGetters.get(property);
		return getter == null //
				? Stream.empty()
				: sources.get().stream().map(s -> State.readProperty(getter, s)).flatMap(Optional::stream);
	}

	List<Map<EnvironmentProperty, Optional<String>>> allValues() {
		return sources.get().stream().map(source -> {
			Map<EnvironmentProperty, Optional<String>> propertyValues = new HashMap<>(propertyGetters.size());
			propertyGetters.forEach((property, getter) -> {
				Optional<String> value = State.readProperty(getter, source);
				propertyValues.put(property, value);
			});
			return propertyValues;
		}).collect(Collectors.toList());
	}

	abstract List<T> getSource(SystemInfo system);

	static final class Disks extends Swath<HWDiskStore> {

		Disks(SystemInfo info) {
			super(info, Map.of( //
					new Disk.Name(), HWDiskStore::getName, //
					new Disk.Model(), HWDiskStore::getModel, //
					new Disk.Serial(), HWDiskStore::getSerial));
		}

		@Override
		protected List<HWDiskStore> getSource(SystemInfo system) {
			return system.getHardware().getDiskStores();
		}
	}

	static final class Nets extends Swath<NetworkIF> {

		Nets(SystemInfo info) {
			super(info, Map.of( //
					new NetworkInterface.Name(), NetworkIF::getName, //
					new NetworkInterface.DisplayName(), NetworkIF::getDisplayName, //
					new NetworkInterface.MacAddress(), NetworkIF::getMacaddr, //
					new NetworkInterface.HwAddress(), Nets::getNetHardwareAddress));
		}

		@Override
		protected List<NetworkIF> getSource(SystemInfo system) {
			return system.getHardware().getNetworkIFs();
		}

		private static String getNetHardwareAddress(NetworkIF net) {
			try {
				byte[] bytes = net.queryNetworkInterface().getHardwareAddress();
				return IntStream.range(0, bytes.length).map(i -> bytes[i])
						.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
			} catch (Exception e) {
				return null;
			}
		}
	}

}
