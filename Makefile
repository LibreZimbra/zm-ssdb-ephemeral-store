# SPDX-License-Identifier: AGPL-3.0-or-later

ANT_TARGET = dist

all: build-ant-autover

include build.mk

install:
	$(call mk_install_dir, lib/ext/com_zimbra_ssdb_ephemeral_store)
	cp build/dist/*.jar $(INSTALL_DIR)/lib/ext/com_zimbra_ssdb_ephemeral_store/

clean: clean-ant
