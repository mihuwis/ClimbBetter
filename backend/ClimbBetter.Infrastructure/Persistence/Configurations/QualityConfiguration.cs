using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using ClimbBetter.Domain.Training;

namespace ClimbBetter.Infrastructure.Persistence.Configurations;

public class QualityConfiguration : IEntityTypeConfiguration<Quality>
{
    public void Configure(EntityTypeBuilder<Quality> builder)
    {
        builder.ToTable("Qualities");

        builder.HasData(
            new Quality { Id = 1, Name = "Onsight", Code = "OS", Multiplier = 1.20m },
            new Quality { Id = 2, Name = "Flash", Code = "FL", Multiplier = 1.10m },
            new Quality { Id = 3, Name = "Redpoint", Code = "RP", Multiplier = 1.00m },
            new Quality { Id = 4, Name = "Repeat RP", Code = "RRP", Multiplier = 0.80m },
            new Quality { Id = 5, Name = "Attempt", Code = "ATT", Multiplier = 1.00m },
            new Quality { Id = 6, Name = "Repeat TR", Code = "RTR", Multiplier = 0.50m }
        );
    }
}