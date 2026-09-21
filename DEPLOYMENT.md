# Backend deployment

## Docker image

Build the production image from the backend repository root:

```shell
docker build -t dinehub-backend .
```

Run it with runtime configuration supplied as environment variables. Never
copy a real `.env` file into the image:

```shell
docker run --rm -p 9090:9090 --env-file .env dinehub-backend
```

The image uses Java 17, builds the Spring Boot JAR in a separate build stage,
and runs the application as a non-root user. Hosted platforms can supply
`JAVA_TOOL_OPTIONS` if JVM memory tuning is required.

## Environment variables

The following production values must be configured in the hosting platform:

- `DATABASE_URL`: the complete Neon connection string beginning with
  `postgresql://`. DineHub converts it to JDBC format at startup. You can
  instead use `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` separately.
- `JWT_SECRET`: a strong random secret of at least 32 bytes
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `FRONTEND_URL`: the deployed frontend origin used by Stripe redirects
- `PAYMENT_CURRENCY`: defaults to `inr`

For Render with Neon, add the Neon connection string as `DATABASE_URL` in the
web service's Environment page. Keep `sslmode=require` in that URL and do not
surround the value with quotes. Platforms such as Render and Railway normally
provide `PORT` automatically; the application defaults to `9090` when it is
absent.

Use `.env.example` as the variable template. Do not commit `.env` or any live
credentials.
