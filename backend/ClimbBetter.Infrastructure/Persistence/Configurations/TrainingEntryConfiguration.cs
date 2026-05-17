using ClimbBetter.Domain.Training;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace ClimbBetter.Infrastructure.Persistence.Configurations;

public class TrainingEntryConfiguration : IEntityTypeConfiguration<TrainingEntry>
{
    public void Configure(EntityTypeBuilder<TrainingEntry> builder)
    {
        builder.ToTable("TrainingEntries");

        builder.HasKey(x => x.Id);

        builder.Property(x => x.AscentCategory)
            .HasConversion<string>()
            .HasMaxLength(50)
            .IsRequired();

        builder.Property(x => x.ExecutedMoves)
            .IsRequired();

        builder.Property(x => x.LengthMultiplierSnapshot)
            .HasPrecision(10, 2);

        builder.Property(x => x.QualityMultiplierSnapshot)
            .HasPrecision(10, 2);

        builder.Property(x => x.PointsSnapshot)
            .HasPrecision(12, 2);

        builder.Property(x => x.Notes)
            .HasMaxLength(1000);

        builder.Property(x => x.CreatedAt)
            .IsRequired();

        builder.HasOne(x => x.TrainingSession)
            .WithMany(x => x.Entries)
            .HasForeignKey(x => x.TrainingSessionId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.HasOne(x => x.Climb)
            .WithMany()
            .HasForeignKey(x => x.ClimbId)
            .OnDelete(DeleteBehavior.Restrict);

        builder.HasOne(x => x.Difficulty)
            .WithMany()
            .HasForeignKey(x => x.DifficultyId)
            .OnDelete(DeleteBehavior.Restrict);

        builder.HasOne(x => x.Quality)
            .WithMany()
            .HasForeignKey(x => x.QualityId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}