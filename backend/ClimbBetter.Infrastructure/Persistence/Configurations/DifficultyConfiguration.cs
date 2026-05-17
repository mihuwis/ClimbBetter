using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using ClimbBetter.Domain.Training;

namespace ClimbBetter.Infrastructure.Persistence.Configurations;

public class DifficultyConfiguration : IEntityTypeConfiguration<Difficulty>
{
    public void Configure(EntityTypeBuilder<Difficulty> builder)
    {
        builder.ToTable("Difficulties");

        builder.HasData(
            new Difficulty { Id = 1, Name = "5c", Points = 55 },
            new Difficulty { Id = 2, Name = "6a", Points = 60 },
            new Difficulty { Id = 3, Name = "6a+", Points = 65 },
            new Difficulty { Id = 4, Name = "6b", Points = 70 },
            new Difficulty { Id = 5, Name = "6b+", Points = 75 },
            new Difficulty { Id = 6, Name = "6c", Points = 80 },
            new Difficulty { Id = 7, Name = "6c+", Points = 85 },
            new Difficulty { Id = 8, Name = "7a", Points = 90 }
        );
    }
}