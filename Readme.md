# AWS CloudWatch RUM Event Visializer (Proof-of-concept)

Visualize CloudWatch RUM events from a Webapp Frontend as spans in a timeline.  
Uses Jaeger as a UI and tracing backend.

The data in `./test/` is extracted from an AWS LogGroup for RUM.

## Running it

Install `maven` and `docker` first.

Then run:

1. `./start-ui.sh`
2. `./build-and-import.sh`

Open the Jaeger UI on localhost. Set the time range to a custom time, and set the start year to 2000 or something early.

## License

MIT

