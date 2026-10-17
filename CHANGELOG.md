# Changelog

## [3.1.1](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/compare/v3.1.0...v3.1.1) (2026-10-17)


### 🐛 Bug Fixes

* **deps:** Batch Dependabot updates, including async-http-client and okhttp CVE fixes ([#13](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/issues/13)) ([4e661c4](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/4e661c4dfdf9c091b1077470380e2d895386b729))

## [3.1.0](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/compare/v3.0.4...v3.1.0) (2026-10-16)


### Features

* Add local development authentication support ([44498a8](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/44498a8a8ab451cb6e21ffb388be02d5200c8356))
* **ci:** add GitHub Actions build, release, and publish workflows ([13583f1](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/13583f1c029052c235a6ad67162ced0a61c3c971))


### Bug Fixes

* add default value for cosmos.maxConnectionsPerEndpoint to maintain backward compatibility ([abe2d85](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/abe2d856e41c42b64a4667f58a10cb5d3b760cb1))
* add default value for cosmos.maxConnectionsPerEndpoint to maintain backward compatibility ([5e118f3](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/5e118f33f93842385e8388f3279f6d783d25d7df))
* add timeout to Redis MSI credentials provider ([fa44729](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/fa4472922e697db19c0d6982cf55d2234f1e4f25))
* add timeout to Redis MSI credentials provider ([b1f3aa4](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/b1f3aa41758dbef2ea9ad6093c2d07c6b78c5392))
* address PR feedback on local dev credential security ([f6c69b3](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/f6c69b34c89e458a4f892d017e3beea7b74b4184))
* close local-dev credential backdoor vectors ([cdbe8c8](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/cdbe8c843ba07ad9ad25b76a2c33bd203d579d3a))
* **dependencies:** update patch-level dependencies for April 2025 ([2ebf404](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/2ebf4045982968f8c7910d65e6a9d9fa221454c0))
* **dependencies:** update patch-level dependencies for April 2025 ([171b383](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/171b383192c8849338e0e8d8b6538332c1f4b1d2))
* **deps:** security patches and test coverage improvements ([e051cbb](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/e051cbb223a904ece79a1493f1e69cffff0062de))
* **deps:** security patches and test coverage improvements ([9c7d4fb](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/9c7d4fba128ba5f12d66a9d177089a335b719e3a))
* guard against null ThreadContext in LogCustomDimensionFilter ([0a344ff](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/0a344ff40381a52cdf1a854413eca184a9ac68ad))
* guard against null ThreadContext in LogCustomDimensionFilter ([f96e9c9](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/f96e9c9b92f44476deca324a3c2de74b0c5b0b31))
* NOTICE ([99cc691](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/99cc6918a05296ee455016de9f64f4cc103b16c3))
* NOTICE ([68aced7](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/68aced75268d33cca0694a1c5030af6c56a82e18))
* replace wildcard imports with explicit imports in local dev tests ([333b229](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/333b2296e842c5ddc3bee5ac6618a151a9c3c0a9))
* request one scope for workload identity tokens ([360ff84](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/360ff84ea018e568125fc2743dec4083436b6ccc))
* restore LogCustomDimensionFilter accidentally removed in MR !399 ([ecaea9e](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/ecaea9e8cc2988a5507d31551c405041bd610651))
* restore LogCustomDimensionFilter accidentally removed in MR !399 ([6210d6b](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/commit/6210d6bb850c79327e61d7f5c9b280c350b5a5d1))
