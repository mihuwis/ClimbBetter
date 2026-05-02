using Microsoft.EntityFrameworkCore;

namespace ClimbBetter.Infrastructure.Persistence;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.HasDefaultSchema("training");
        base.OnModelCreating(modelBuilder);
    }
}