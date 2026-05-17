using Microsoft.EntityFrameworkCore;
using ClimbBetter.Domain.Training;


namespace ClimbBetter.Infrastructure.Persistence;

public class ClimbBetterDbContext : DbContext
{

    public DbSet<Location> Locations => Set<Location>();
    public DbSet<Climb> Climbs => Set<Climb>();
    public DbSet<Difficulty> Difficulties => Set<Difficulty>();
    public DbSet<Quality> Qualities => Set<Quality>();
    public DbSet<TrainingSession> TrainingSessions => Set<TrainingSession>();
    public DbSet<TrainingEntry> TrainingEntries => Set<TrainingEntry>();


    public ClimbBetterDbContext (DbContextOptions<ClimbBetterDbContext> options) : base(options)
    {
    }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.HasDefaultSchema(DatabaseSchemas.Training);
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(ClimbBetterDbContext).Assembly);
        
        base.OnModelCreating(modelBuilder);
    }
}