type VersionRouteOptions = {
  sha: string;
  buildTime: string;
};

export async function registerVersionRoute(app: any, options: VersionRouteOptions): Promise<void> {
  app.get("/version", async () => ({
    sha: options.sha,
    buildTime: options.buildTime
  }));
}
